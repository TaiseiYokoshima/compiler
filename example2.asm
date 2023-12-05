.macro Terminate
	lw 	          a7, exit_code
	lw            a0, (fp)
	ecall
.end_macro

.macro    PushImm     $number
    li            t1, $number
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    PushImmNeg     $number
    li            t1, -$number
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro PushVar $index
	#calculates offset
	li 	t0, 12
	li	t1, 4
	li	t2, $index
	mul	t1, t1, t2
	add	t0, t0, t1

	#turns it to negative
	li	t1, -1
	mul	t0, t0, t1

	#applies the offset
	add	t0, t0, fp

	#retrives the value at offset + fp to  t0
	lw	t0, 0(t0)

	#saves the value to sp
	sw	t0, (sp)
	addi	sp, sp, -4
.end_macro



.macro    PushRel     $offset
    lw            t1, -$offset(fp)
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    PopRel      $offset
    lw            t1, 4(sp)
    addi          sp, sp, 4
    sw            t1, -$offset(fp)
.end_macro

.macro    Reserve
    addi          sp, sp, -4
.end_macro

.macro    Discard
    addi          sp, sp, 4
.end_macro

.macro    SetFP
    mv            fp, sp
.end_macro

.macro    SaveFP
    sw            fp, (sp)
    addi          sp, sp, -4
.end_macro

.macro    RestoreFP
    lw            fp, 4(sp)
    addi          sp, sp, 4
.end_macro

.macro	Popt1t2
    lw	        t2, 4(sp)
    lw	        t1, 8(sp)
    addi	    sp, sp, 8
.end_macro

.macro	CompGT $s
    Popt1t2
    li	t0, 0
    blt	t1, t2, compgt_exit$s
    beq	t1, t2, compgt_exit$s
    li	t0, 1
compgt_exit$s:
    sw	t0, (sp)
    addi sp, sp, -4
.end_macro

.macro    CompGE  $s
    Popt1t2
    li            t0, 0
    blt           t1, t2, compge_exit$s
    li            t0, 1
compge_exit$s:
    sw            t0, (sp)
    addi          sp, sp, -4
.end_macro

.macro    CompEQ
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    beq           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro Invert $s
    lw	t1, 4(sp)
    li	t0, 1
    beqz	t1, invert_exit$s
    li	t0, 0
invert_exit$s:
    sw	t0, 4(sp)
.end_macro

.macro OrBinop
    Popt1t2
    or t1, t1, t2
    sw t1, (sp)
    addi sp, sp, -4
.end_macro

.macro AndBinop
    Popt1t2
    and t1, t1, t2
    sw t1, (sp)
    addi sp, sp, -4
.end_macro

.macro XorBinop
    Popt1t2
    xor t1, t1, t2
    sw t1, (sp)
    addi sp, sp, -4
.end_macro

.macro Plus
    Popt1t2
    add	t0, t1, t2
    sw	t0, (sp)
    addi	sp, sp, -4
.end_macro

.macro Minus
    Popt1t2
    sub	t0, t1, t2
    sw	t0, (sp)
    addi	sp, sp, -4
.end_macro

.macro    Times
    Popt1t2
    mul           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    Divide
    Popt1t2
    div           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    Jump        $address
    j            $address
.end_macro

.macro    JumpTrue    $address
    lw            t1, 4(sp)
    addi          sp, sp, 4
    beqz          t1, exit
    j             $address
exit:
.end_macro

.macro JumpToElse $label_name
	#consume the value of the logical binary operation
	lw	t0, 4(sp)
	addi	sp, sp, 4
	#jumps to else label if the condition is zero
	beqz	t0, $label_name
.end_macro

.macro ArgSetStart
	addi	sp, sp, -12
.end_macro

.macro ArgSetEnd  $num_of_args
	li	t1, $num_of_args
	li	t2, 4
	mul	t1, t1, t2
	addi	t1, t1, 12
	add	sp, sp, t1
.end_macro

.macro PrepareFunc $num_of_args
	sw 	fp, -4(sp)
	SetFP
	PushImm	0
	addi 	sp, sp, -8

	li	t0, 4
	li	t1, $num_of_args
	mul	t0, t0, t1
	sub	sp, sp, t0
.end_macro

.macro Invoke $func_name
	jal 	next
next:
	mv	t1, ra
	addi 	t1, t1, 16
	sw	t1, -8(fp)
	j	$func_name
.end_macro

.macro Return
	#retrieves the top of the stack and stores it to rv
	lw	t0, 4(sp)
	sw	t0, 0(fp)
	#retrieves ra
	lw	ra, -8(fp)
	#resets sp
	addi	sp, fp, -4
	#retrives previous fp and set it to fp
	lw 	fp, -4(fp)
	jalr	zero, ra, 0
.end_macro

.macro	Print
	lw 	a7, print_int_code
	lw	a0, 4(sp)
	ecall
	PrintNewLine
.end_macro

.macro    PrintSpace
    li            a7, 11
    li            a0, 32
    ecall
.end_macro

.macro PrintNewLine
	lw	a7, print_str_code
	la	a0, newline
	ecall
.end_macro
.data
    n: .word 100
    terminate: .string "terminated with: "
    print_func: .string "terminated func with: "
    newline: .string "\n"

    exit_code: .word 93
    print_int_code: .word 1
    print_str_code: .word 4

# code below:
.text
    # bootstrap loader that runs main()
boot:
    SetFP

    PrepareFunc     0
    Invoke      main
    Terminate
main:
    ArgSetStart
    PushImm     10
    ArgSetEnd 1
    PrepareFunc 1
    Invoke      fibo
    Return
fibo:
cond_label_0:
    PushVar     0
    PushImm     2
    CompGE      4
    Invert
    JumpToElse    else_label_2

then_label_1:
    PushVar     0
    j        end_if_label_3
else_label_2:
    ArgSetStart
    PushVar     0
    PushImm     1
    Minus
    ArgSetEnd 1
    PrepareFunc 1
    Invoke      fibo
    ArgSetStart
    PushVar     0
    PushImm     2
    Minus
    ArgSetEnd 1
    PrepareFunc 1
    Invoke      fibo
    Plus
end_if_label_3:
    Return