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

.macro PushVarBlock    $index
    #calculates offset
    li      t0, 8
    li,     t1, 4
    li,     t2, $index
    mul     t1, t1, t2
    add     t0, t0, t1

    #turns it to negative
    li      t1, -1
    mul     t0, t0, t1

    #applies the offset
    add     t0, t0, s1

    #retrives the value at offset + s1 to to
    lw      t0, 0(t0)

    #saves the value to sp
    sw      t0, (sp)
    addi    sp, sp, -4
.end_macro

.macro PushVarBlockID    $index
    #calculates offset
    li      t0, 8
    li,     t1, 4
    li,     t2, $index
    mul     t1, t1, t2
    add     t0, t0, t1

    #turns it to negative
    li      t1, -1
    mul     t0, t0, t1

    #applies the offset
    add     t0, t0, t6

    #retrives the value at offset + s1 to to
    lw      t0, 0(t0)

    #saves the value to sp
    sw      t0, (sp)
    addi    sp, sp, -4
.end_macro


.macro    PushRel     $offset
    lw            t1, -$offset(fp)
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    PopRel      $index
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

	#retrives the value at the top of stack
	addi	sp, sp, 4
	lw	    t1, (sp)

	#saves the value to the var
	sw	t1, (t0)
.end_macro

.macro    PopRelBlock      $index
	#calculates offset
	li 	t0, 8
	li	t1, 4
	li	t2, $index
	mul	t1, t1, t2
	add	t0, t0, t1

	#turns it to negative
	li	t1, -1
	mul	t0, t0, t1

	#applies the offset
	add	t0, t0, s1

	#retrives the value at the top of stack
	addi	sp, sp, 4
	lw	    t1, (sp)

	#saves the value to the var
	sw	t1, (t0)
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

.macro    CompEQ    $s
    Popt1t2
    li	t0, 1
    beq	t1, t2, compeq_exit$s
    li	t0, 0
compeq_exit$s:
    sw	t0, (sp)
    addi sp, sp, -4
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
    div           t0, t1, t2
    sw            t0, (sp)
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

.macro JumpToExit $label_name
	#consume the value of the logical binary operation
	lw	t0, 4(sp)
	addi	sp, sp, 4
	#jumps to exit label if the condition is zero
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
	addi    sp, sp, 4

	#PrintNewLine
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

    exit_code: .word 10
    print_int_code: .word 1
    print_str_code: .word 4

# code below:
.text
    # bootstrap loader that runs main()
boot:
    SetFP

    ArgSetStart
    PushImm     9097
    ArgSetEnd     1
    PrepareFunc     1
    Invoke      main
    Terminate
main:
    PushImm     2
    PushVar     0
    PushImm 0
    PushImm	0
while_cond_label_0:
    PushVar     1
    PushVar     0
    CompGT      3
    Invert      4
    PushVar     2
    PushImm     1
    CompGT      5
    AndBinop
    JumpToExit  while_exit_label_2
while_block_label_1:
	PushImm		0
while_cond_label_6:
    PushVar     2
    PushVar     2
    PushVar     1
    Divide
    PushVar     1
    Times
    Minus
    PushImm     0
    CompEQ      9
    JumpToExit  while_exit_label_8
while_block_label_7:
    PushImm 1
    PopRel      (3)
    Reserve
    Discard
    #print expression
    PushVar     1
    Print
    Reserve
    Discard
    #print expression
    PrintSpace
    Reserve
    Discard
    PushVar     2
    PushVar     1
    Divide
    PopRel      (2)
    Reserve
    Discard
    j   while_cond_label_6
while_exit_label_8:
    Discard
cond_label_10:
    PushVar     3
    JumpToElse    else_label_12

then_label_11:
    #print expression
    PrintNewLine
    Reserve
    Discard
    PushImm 0
    PopRel      (3)
    Reserve
    j        end_if_label_13
else_label_12:
    Reserve
end_if_label_13:
    Discard
    PushVar     1
    PushImm     1
    Plus
    PopRel      (1)
    Reserve
    Discard
    j   while_cond_label_0
while_exit_label_2:
    Discard
    PushImm     0
    Return