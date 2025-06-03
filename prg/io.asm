.data
_buffer: .zero 255

# Copyright (c) 2025 Prev25 Wizards
.text
_start: # real entrypoint of asm prg
    # set some regs
    li gp, 0 # should be data
    li tp, 0x0000000200000000 # heap
    li sp, 0x00000000FFFFFFF0 # stack
    mv s0, sp # fp = s0
    call _main  # jump to target and save position to ra
    ld a0, 0(sp)
    li a7, 93
    ecall

.text
_main:
    addi sp,sp,-64
    sd ra,48(sp)
    sd s0,40(sp)
    addi s0,sp,64
L0:
    sd s0,0(sp)
    call _getchar
    mv t1,a0
    sd s0,0(sp)
    sd t1,8(sp)
    call _putchar
    mv t1,a0
    sd s0,0(sp)
    li t1,10
    sd t1,8(sp)
    call _putchar
    mv t1,a0
    sd s0,0(sp)
    call _getint
    mv t1,a0
    sd s0,0(sp)
    sd t1,8(sp)
    call _putint
    mv t1,a0
    sd s0,0(sp)
    li t1,10
    sd t1,8(sp)
    call _putchar
    mv t1,a0
    sd s0,0(sp)
    call _getint
    mv t1,a0
    sd s0,0(sp)
    sd t1,8(sp)
    call _putint
    mv t1,a0
    sd s0,0(sp)
    li t1,10
    sd t1,8(sp)
    call _putchar
    mv t1,a0
    sd s0,0(sp)
    call _getint
    mv t1,a0
    sd s0,0(sp)
    sd t1,8(sp)
    call _putint
    mv t1,a0
    sd s0,0(sp)
    li t1,10
    sd t1,8(sp)
    call _putchar
    mv t1,a0
    li t2,0
    li t1,1
    mul t1,t2,t1
    la t2,_buffer
    add t1,t2,t1
    sd s0,0(sp)
    sd t1,8(sp)
    li t1,255
    sd t1,16(sp)
    call _gets
    mv t1,a0
    sd s0,0(sp)
    sd t1,8(sp)
    call _putint
    mv t1,a0
    sd s0,0(sp)
    li t1,10
    sd t1,8(sp)
    call _putchar
    mv t1,a0
    li t1,0
    li t2,1
    mul t2,t1,t2
    la t1,_buffer
    add t1,t1,t2
    sd s0,0(sp)
    sd t1,8(sp)
    call _puts
    mv t1,a0
    li t1,0
    mv a0,t1
L1:
    ld s0,40(sp)
    ld ra,48(sp)
    addi sp,sp,64
    jr ra

.data
buffer:     .zero 8

# Copyright 2025 Prev25 Wizards
.text # Wrappers for RIPES system calls

# Exit Program
.align 4
.global _die
_die:
    li a0, 1      
    li a7, 93     
    ecall

# Exit Program with code
.align 4
.global _exit
_exit:
    li a7, 93
    ecall

# Print Integer
.align 4
.global _putint
_putint:
    # save regs
    sd a0, -8(sp)
    sd a7, -16(sp)

    # actual call
    ld a0, 8(sp)
    li a7, 1
    ecall

    # restore regs
    ld a0, -8(sp)
    ld a7, -16(sp)

    ret

# Print String
.align 4
.global _puts
_puts:
    # save regs
    sd a0, -8(sp)
    sd a7, -16(sp)

    # actual call
    ld a0, 8(sp)
    li a7, 4
    ecall

    # restore regs
    ld a0, -8(sp)
    ld a7, -16(sp)

    ret

# Print Character
.align 4
.global _putchar
_putchar:
    # save regs
    sd a0, -8(sp)
    sd a7, -16(sp)

    # actual call
    ld a0, 8(sp)
    li a7, 11
    ecall

    # restore regs
    ld a0, -8(sp)
    ld a7, -16(sp)

    ret

# Print Integer as Hex
.align 4
.global _putint_hex
_putint_hex:
    # save regs
    sd a0, -8(sp)
    sd a7, -16(sp)

    # actual call
    ld a0, 8(sp)
    li a7, 34
    ecall

    # restore regs
    ld a0, -8(sp)
    ld a7, -16(sp)

    ret

# Print Integer as Binary
.align 4
.global _putint_bin
_putint_bin:
    # save regs
    sd a0, -8(sp)
    sd a7, -16(sp)

    # actual call
    ld a0, 8(sp)
    li a7, 35
    ecall

    # restore regs
    ld a0, -8(sp)
    ld a7, -16(sp)

    ret

# Print Integer as Unsigned
.align 4
.global _putuint
_putuint:
    # save regs
    sd a0, -8(sp)
    sd a7, -16(sp)

    # actual call
    ld a0, 8(sp)
    li a7, 36
    ecall

    # restore regs
    ld a0, -8(sp)
    ld a7, -16(sp)

    ret

# obtains char from stdin
.align 4
.global _getchar
_getchar:
    addi sp, sp, -32        # make room on stack (aligned to 16 bytes)

    # save registers
    sd a0, 0(sp)
    sd a1, 8(sp)
    sd a2, 16(sp)
    sd a7, 24(sp)

    li a7, 63               # syscall: read
    li a0, 0                # fd = stdin
    addi a1, sp, -1         # buffer at sp-1 (any location you control)
    sd x0, 0(a1)            # clear buffer
    li a2, 1                # read 1 byte
    ecall

    lbu a0, 0(a1)           # load byte read into a0

    # restore registers
    ld a1, 8(sp)
    ld a2, 16(sp)
    ld a7, 24(sp)
    addi sp, sp, 32         # restore stack pointer

    ret


# obtains string from stdin
.align 4
.global _gets # (ptr, len) -> bytes read
_gets:
  # save regs
  sd a0, -8(sp)
  sd a1, -16(sp)
  sd a2, -24(sp)
  sd a7, -32(sp)

  li a7, 63 # read sys call
  li a0, 0 # stdin
  ld a1, 8(sp) # ptr
  ld a2, 16(sp) # max buf read
  addi a2, a2, -1 # for nul byte
  ecall

  add a1, a1, a0
  sb x0, 1(a1)
  sd a0, 0(sp)

  # restore regs
  ld a0, -8(sp)
  ld a1, -16(sp)
  ld a2, -24(sp)
  ld a7, -32(sp)

  ret

# Read a 64-bit signed integer from stdin, char by char
.align 4
.global _getint
_getint:
    addi sp, sp, -32
    sd ra, 24(sp)
    sd s0, 16(sp)
    sd s1, 8(sp)
    sd s2, 0(sp)

    # read syscall: read 32 bytes into buffer from stdin
    li a0, 0          # fd 0 = stdin
    la a1, buffer     # buffer address
    li a2, 32         # read up to 32 bytes
    li a7, 63         # syscall read
    ecall

    la s0, buffer     # s0 = pointer to buffer
    li s1, 0          # result = 0
    li s2, 0          # sign = 0 (positive)

SkipWhitespace:
    lbu t0, 0(s0)
    beqz t0, ParseDone
    li t1, 32         # space
    beq t0, t1, SkipAdvance
    li t1, 9          # tab
    beq t0, t1, SkipAdvance
    j ParseSign

SkipAdvance:
    addi s0, s0, 1
    j SkipWhitespace

ParseSign:
    lbu t0, 0(s0)
    li t1, 45         # '-'
    beq t0, t1, IsNegative
    li t1, 43         # '+'
    beq t0, t1, IsPositive
    j ParseDigits

IsNegative:
    li s2, 1          # negative sign
    addi s0, s0, 1
    j ParseDigits

IsPositive:
    addi s0, s0, 1
    j ParseDigits

ParseDigits:
    lbu t0, 0(s0)
    li t1, 10
    beq t0, t1, ParseDone  # newline
    beqz t0, ParseDone     # null terminator

    li t2, 48
    li t3, 57
    blt t0, t2, ParseDone
    bgt t0, t3, ParseDone

    # digit = t0 - '0'
    addi t0, t0, -48
    li t4, 10
    mul s1, s1, t4
    add s1, s1, t0

    addi s0, s0, 1
    j ParseDigits

ParseDone:
    # if negative, negate s1
    beqz s2, Return
    neg s1, s1

Return:
    mv a0, s1         # return value in a0
    ld ra, 24(sp)
    ld s0, 16(sp)
    ld s1, 8(sp)
    ld s2, 0(sp)
    addi sp, sp, 32
    ret

.align 4
.global _new #(size in bytes) -> ptr
_new:
.align 4
.global _malloc #(size in bytes) -> ptr
_malloc:
    sd a0, -8(sp)

    # Load requested size from stack
    ld a0, 8(sp)

    # Align size to 8 bytes
    addi a0, a0, 7
    andi a0, a0, -8

    # Allocate memory
    mv a1, tp          # a1 = current heap pointer (return value)
    add tp, tp, a0

    # Return allocated pointer in a0
    mv a0, a1

    # Restore original a0 (optional, might be overwritten above)
    # ld a0, -8(sp)

    ret

.align 4
.global _del
_del:
.align 4
.global _free
_free:
    # Save register a0 in case caller cares
    sd a0, -8(sp)

    # Load argument from stack (pointer to free)
    ld a0, 8(sp)

    # Normally we'd do something like deallocate memory at a0
    # But in this context (e.g., RIPES), it's a no-op
    # So we just discard it

    # Restore original a0
    ld a0, -8(sp)
    ret
