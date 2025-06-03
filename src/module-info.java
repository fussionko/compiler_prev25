/**
 * The compiler for the Prev25 programming language.
 * 
 * The compiler should be run as
 * 
 * <p>
 * {@code $ java compiler.Compiler} <i>command-line-options...</i>
 * </p>
 * 
 * The following command line options are available:
 * 
 * <ul>
 * 
 * <li>{@code --src-file-name=}<i>file-name</i>: The name of the source file,
 * i.e., the file containing the code to be compiled.</li>
 * 
 * <li>{@code --dst-file-name=}<i>file-name</i>: The name of the destination
 * file, i.e., the file containing the compiled code.</li>
 * 
 * <li>{@code --target-phase=}<i>phase-name</i>: The name of the last phase to
 * be performed, or {@code none} or {@code all} (default).</li>
 * 
 * <li>{@code --logged-phase=}<i>phase-name</i>: The name of the phase the
 * report file is to be generated for, or {@code none} or {@code all}
 * (default).</li>
 *
 * <li>{@code --xml=}<i>prefix</i>: The prefix of the name of the generated xml
 * report files (unless specified the base name of the source file name is
 * used).</li>
 * 
 * <li>{@code --xsl=}<i>dir-name</i>: The directory where xsl templates used by
 * generated xml report files are stored.</li>
 * 
 * <li>{@code --dev-mode=}{@code on}|{@code off}: Development mode (browse the
 * source code for information).
 * 
 * </ul>
 * 
 * The source file can be specified by its name only, i.e., without
 * {@code --src-file-name}. If the source file is not specified, the last
 * modified .prev25 file found in the working directory is used. Unless the name
 * of the destination file is specified, the base name of the source file name
 * is used.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
module compiler {
	requires java.xml;
	requires antlr;
}
