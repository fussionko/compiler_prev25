package compiler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

import compiler.common.report.*;
import compiler.phase.lexan.*;
import compiler.phase.livean.*;
import compiler.phase.synan.*;
import compiler.phase.abstr.*;
import compiler.phase.seman.*;
import compiler.phase.memory.*;
import compiler.phase.imcgen.*;
import compiler.phase.imclin.*;
import compiler.phase.imclin.LIN.DataChunk;
import compiler.phase.asmgen.*;
import compiler.phase.regall.*;


/**
 * The Prev25 compiler.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Compiler {

	/** (Unused but included to keep javadoc happy.) */
	private Compiler() {
		throw new Report.InternalError();
	}

	/** Names of command line options. */
	private static final HashSet<String> cmdLineOptNames = new HashSet<String>(Arrays.asList("--src-file-name",
			"--dst-file-name", "--target-phase", "--logged-phase", "--xml", "--xsl", "--dev-mode", "--num-regs"));

	/** Values of command line options indexed by their command line option name. */
	private static final HashMap<String, String> cmdLineOptValues = new HashMap<String, String>();

	/** All valid phases name of the compiler. */
	private static final Vector<String> phaseNames = new Vector<String>(
			Arrays.asList("none", "all", "lexan", "synan", "abstr", "seman", "memory", "imcgen", "imclin", "asmgen", "livean", "regall"));

	/**
	 * Returns the value of a command line option.
	 *
	 * @param cmdLineOptName Command line option name.
	 * @return Command line option value.
	 */
	public static final String cmdLineOptValue(final String cmdLineOptName) {
		return cmdLineOptValues.get(cmdLineOptName);
	}

	/** Specifies whether the compiler is run in the development mode. */
	private static boolean devMode = false;

	/**
	 * Returns information on whether the compiler is run in the development mode.
	 * 
	 * @return {@code true} if the compiler is run in the development mode,
	 *         {@code false} otherwise.
	 */
	public static final boolean devMode() {
		return devMode;
	}

	/**
	 * The compiler's main driver running all phases one after another.
	 * 
	 * @param opts Command line arguments (see {@link compiler}).
	 */
	public static void main(final String[] opts) {
		try {
			Report.info("This is Prev25 compiler:");

			// Scan the command line.
			for (int optc = 0; optc < opts.length; optc++) {
				if (opts[optc].startsWith("--")) {
					// Command line option.
					final String cmdLineOptName = opts[optc].replaceFirst("=.*", "");
					final String cmdLineOptValue = opts[optc].replaceFirst("^[^=]*=", "");
					if (!cmdLineOptNames.contains(cmdLineOptName)) {
						Report.warning("Unknown command line option '" + cmdLineOptName + "'.");
						continue;
					}
					if (cmdLineOptValues.get(cmdLineOptName) == null) {
						// Not yet successfully specified command line option.

						// Check the value of the command line option.
						if (cmdLineOptName.equals("--target-phase") && (!phaseNames.contains(cmdLineOptValue))) {
							Report.warning("Illegal phase specification in '" + opts[optc] + "' ignored.");
							continue;
						}
						if (cmdLineOptName.equals("--logged-phase") && (!phaseNames.contains(cmdLineOptValue))) {
							Report.warning("Illegal phase specification in '" + opts[optc] + "' ignored.");
							continue;
						}
						if (cmdLineOptName.equals("--dev-mode") && (!cmdLineOptValue.matches("on|off"))) {
							Report.warning("Illegal value in '" + opts[optc] + "' ignored.");
							continue;
						}

						cmdLineOptValues.put(cmdLineOptName, cmdLineOptValue);
					} else {
						// Repeated specification of a command line option.
						Report.warning("Command line option '" + opts[optc] + "' ignored.");
						continue;
					}
				} else {
					// Source file name.
					if (cmdLineOptValues.get("--src-file-name") == null) {
						cmdLineOptValues.put("--src-file-name", opts[optc]);
					} else {
						Report.warning("Source file '" + opts[optc] + "' ignored.");
						continue;
					}
				}
			}
			// Check the command line option values.
			if (cmdLineOptValues.get("--src-file-name") == null) {
				try {
					// Source file has not been specified, so consider using the last modified
					// prev25 file in the working directory.
					final String currWorkDir = new File(".").getCanonicalPath();
					FileTime latestTime = FileTime.fromMillis(0);
					Path latestPath = null;
					for (final Path path : java.nio.file.Files.walk(Paths.get(currWorkDir))
							.filter(path -> path.toString().endsWith(".prev25")).toArray(Path[]::new)) {
						final FileTime time = Files.getLastModifiedTime(path);
						if (time.compareTo(latestTime) > 0) {
							latestTime = time;
							latestPath = path;
						}
					}
					if (latestPath != null) {
						cmdLineOptValues.put("--src-file-name", latestPath.toString());
						Report.warning("Source file not specified, using '" + latestPath.toString() + "'.");
					}
				} catch (final IOException __) {
					throw new Report.Error("Source file not specified.");
				}

				if (cmdLineOptValues.get("--src-file-name") == null) {
					throw new Report.Error("Source file not specified.");
				}
			}
			if (cmdLineOptValues.get("--dst-file-name") == null) {
				cmdLineOptValues.put("--dst-file-name",
						// TODO: Insert the appropriate file suffix.
						cmdLineOptValues.get("--src-file-name").replaceFirst("\\.[^./]*$", ".TODO"));
			}
			if (cmdLineOptValues.get("--target-phase") == null)
				cmdLineOptValues.put("--target-phase", "all");
			if (cmdLineOptValues.get("--logged-phase") == null)
				cmdLineOptValues.put("--logged-phase", "none");
			devMode = ("on".equals(cmdLineOptValues.get("--dev-mode")));

			// Carry out the compilation phase by phase.
			while (true) {

				if (cmdLineOptValues.get("--target-phase").equals("none"))
					break;

				// Lexical analysis.
				if (cmdLineOptValues.get("--target-phase").equals("lexan")) {
					try (final LexAn lexan = new LexAn()) {
						while (lexan.lexer.nextToken().getType() != LexAn.LocLogToken.EOF) {
						}
					}
					break;
				}

				// Syntax analysis.
				try (LexAn lexan = new LexAn(); SynAn synan = new SynAn(lexan)) {
					SynAn.tree = synan.parser.source();
					synan.log(SynAn.tree);
				}
				if (cmdLineOptValues.get("--target-phase").equals("synan"))
					break;

				// Abstract syntax.
				try (Abstr abstr = new Abstr()) {
					Abstr.tree = (AST.Nodes<AST.FullDefn>) SynAn.tree.ast;
					SynAn.tree = null;
					Abstr.Logger logger = new Abstr.Logger(abstr.logger);
					Abstr.tree.accept(logger, "Nodes<Defn>");
				}
				if (cmdLineOptValues.get("--target-phase").equals("abstr"))
					break;

				// Semantic analysis.
				try (SemAn seman = new SemAn()) {
					Abstr.tree.accept(new NameResolver(), null);
					Abstr.tree.accept(new TypeResolver(), null);
					Abstr.tree.accept(new TypeChecker(), null);
					Abstr.Logger logger = new Abstr.Logger(seman.logger);
					logger.addSubvisitor(new SemAn.Logger(seman.logger));
					Abstr.tree.accept(logger, "Nodes<Defn>");
				}
				if (cmdLineOptValues.get("--target-phase").equals("seman"))
					break;

				// Memory.
				try (Memory memory = new Memory()) {
					Abstr.tree.accept(new MemEvaluator(), null);
					Abstr.Logger logger = new Abstr.Logger(memory.logger);
					logger.addSubvisitor(new SemAn.Logger(memory.logger));
					logger.addSubvisitor(new Memory.Logger(memory.logger));
					Abstr.tree.accept(logger, "Nodes<Defn>");
				}
				if (cmdLineOptValues.get("--target-phase").equals("memory"))
					break;

				// Intermediate code generation.
				try (ImcGen imcGen = new ImcGen()) {
					Abstr.tree.accept(new ImcGenerator(), null);
					Abstr.Logger logger = new Abstr.Logger(imcGen.logger);
					logger.addSubvisitor(new SemAn.Logger(imcGen.logger));
					logger.addSubvisitor(new Memory.Logger(imcGen.logger));
					logger.addSubvisitor(new ImcGen.Logger(imcGen.logger));
					Abstr.tree.accept(logger, "AstDefn");
				}
				if (cmdLineOptValues.get("--target-phase").equals("imcgen"))
					break;

				// Linearization of intermediate code.
				try (ImcLin imclin = new ImcLin()) {
					Abstr.tree.accept(new ChunkGenerator(), null);
					imclin.log();

					if (true) {
						Interpreter interpreter = new Interpreter(ImcLin.dataChunks(), ImcLin.codeChunks());
						System.out.println("EXIT CODE: " + interpreter.run("_main"));
					}
				}
				if (cmdLineOptValues.get("--target-phase").equals("imclin"))
					break;
				
				// Generation of processor instructions.
				try (AsmGen asmgen = new AsmGen()) 
				{
					asmgen.addCodeChunks(ImcLin.codeChunks());
					asmgen.generateInstructions();

					//asmgen.log();
					if (false)
					{
						asmgen.printInstructions();
					}
				}
				if (cmdLineOptValues.get("--target-phase").equals("asmgen"))
					break;

				// Liveness analysis
				try (LivenessAnalysis livenessAnalysis = new LivenessAnalysis()) 
				{
					livenessAnalysis.analyseLiveness(AsmGen.codeChunkInstructions);

					//livenessAnalysis.log();
				}
				if (cmdLineOptValues.get("--target-phase").equals("livean"))
					break;

				// Register allocation analysis
				try (Regall regall = new Regall()) 
				{
					String numOfRegistersString = cmdLineOptValues.get("--num-regs");
					Integer numOfRegisters = null;

					if (numOfRegistersString != null)
					{
						try {
					   		numOfRegisters = Integer.parseInt(numOfRegistersString);
						}
						catch (NumberFormatException e) {
					   		numOfRegisters = null;
						}
					}
	
				    regall.allocateRegisters(numOfRegisters, AsmGen.codeChunkInstructions);
					if (false)
					{
						Regall.printInstructions();
					}
					//regall.log();
				}
				if (cmdLineOptValues.get("--target-phase").equals("regall"))
					break;

				if (true)
				{
					generateASMFile();
				}

				// Do not loop... ever.
				break;
			}

			// Let's hope we ever come this far.
			// But beware:
			// 1. The generated translation of the source file might be erroneous :-o
			// 2. The source file might not be what the programmer intended it to be ;-)
			Report.info("Done.");
		} catch (final Report.Error error) {
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

	private static void generateASMFile()
	{
		final String asmFileName = cmdLineOptValue("--src-file-name").replace(".prev25", "") + ".asm";
		Vector<String> lines = new Vector<>();

		// Add data 
		if (true)
		{
			// Data chunk
			final Vector<LIN.DataChunk> dataChunks = ImcLin.dataChunks();
			if (!dataChunks.isEmpty())
			{
				lines.add(".data");
				lines.addAll(getDataLines(dataChunks));
				lines.add("");
			}
		}

		// Add startup code
		if (true)
		{
			final String startFileName = "ripes_start.asm";
			Vector<String> startFileLines = readFile(startFileName);

			lines.addAll(startFileLines);
			lines.add("");
		}

				
		// Add program code
		if (true)
		{
			final Vector<String> codeInstrs = Regall.getInstructions();
			if (!codeInstrs.isEmpty())
			{
				lines.add(".text");
				lines.addAll(codeInstrs);
				lines.add("");
			}
		}

		// Link with library
		if (true)
		{
			final String libFileName = "ripes_sys.asm";
			Vector<String> libFileLines = readFile(libFileName);

			lines.addAll(libFileLines);
		}

		writeFile(asmFileName, lines);
	}

	private static Vector<String> getDataLines(final Vector<LIN.DataChunk> dataChunks)
	{
		Vector<String> lines = new Vector<>();
		for (LIN.DataChunk dataChunk : dataChunks)
		{
			// Get the largest power of two less than or equal to dataChunk.size ,maybe skippable
			// int alignTo = 1;
			// long sz = dataChunk.size;
			// while (alignTo * 2 <= sz) {
			//     alignTo *= 2;
			// }
			// lines.add(".allign " + alignTo
			if (dataChunk.init != null && !dataChunk.init.isEmpty())
			{
				if (dataChunk.isString)
				{
					// String v = dataChunk.init.replace("\n", "\\n");
					// lines.add(dataChunk.label.name + ": .string \"" + v + "\"");
					String v = dataChunk.label.name + ": .byte";
					byte[] bytes = dataChunk.init.getBytes();
					for (int i = 0; i < bytes.length; ++i)
					{
						v += " " + bytes[i] + ",";
					}
					lines.add(v + " 0");
				}
				else
				{
					if (dataChunk.size == 1)
					{
						lines.add(dataChunk.label.name + ": .byte " + dataChunk.init);
					}
					else
					{
						lines.add(dataChunk.label.name + ": .word " + dataChunk.init);
					}
				}
			}
			else 
			{
				lines.add(dataChunk.label.name + ": .zero " + dataChunk.size);
			}
		}
		return lines;
	}

	private static void writeFile(final String fileName, final Vector<String> lines)
	{
		try {
			Path file = Paths.get(fileName);
			Files.write(file, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new Report.Error("Cannot create assembly file: '" + e + "'.");
		}
	}

	private static Vector<String> readFile(final String fileName)
	{
		try {
			Path file = Paths.get(fileName);
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			return new Vector<>(lines);
		} catch (IOException e) {
			throw new Report.Error("Cannot read file: '" + e + "'.");
		}
	}
}
