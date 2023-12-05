.data
    n: .word 100
    terminate: .string "terminated with: "
    print_func: .string "terminated func with: "
    newline: .string "\n"

    exit_code: .word 93
    print_int_code: .word 1
    print_str_code: .word 4

.macro PushImm $int
	li 	t1, $int
	sw, 	t1, (sp)
	addi	sp, sp, -4
.end_macro

.macro PushImmNeg $int
	li 	t1, -$int
	sw, 	t1, (sp)
	addi	sp, sp, -4
.end_macro

.macro Terminate
	lw 	a7, exit_code
	lw	a0, (fp)
	ecall
.end_macro

.macro SetFP
	mv fp, sp
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
	#retrieves the top of the stack and stores it to RV
	lw	t0, 4(sp)
	sw	t0, 0(fp)
	#retrieves RA
	lw	ra, -8(fp)
	#resets sp
	addi	sp, fp, -4
	#retrives previous fp and set it to fp
	lw 	fp, -4(fp)
	jalr	zero, ra, 0
.end_macro
	
	
.macro PrintRegValue $reg
	lw	a7, print_int_code
	mv	a0, $reg
	ecall
	PrintNewLine
.end_macro	
	
.macro PrintNewLine
	lw	a7, print_str_code
	la	a0, newline
	ecall
.end_macro

.macro PrintOffset $offset
	lw	a7, print_int_code
	lw 	a0, -$offset(fp)
	ecall
	PrintNewLine
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

.macro	Print
	lw 	a7, print_int_code
	lw	a0, 4(sp)
	ecall
	PrintNewLine
.end_macro

.macro	Reserve
	addi	sp, sp, -4
.end_macro

.macro	Discard
	addi	sp, sp, 4
.end_macro	

.macro	Popt1t2
	lw	t2, 4(sp)
	lw	t1, 8(sp)
	addi	sp, sp, 8	
.end_macro

.macro	CompGT	$s
	Popt1t2
	li	t0, 0
	blt	t1, t2, compgt_exit$s
	beq	t1, t2, compgt_exit$s
	li	t0, 1
compgt_exit$s:
	sw	t0, (sp)
	addi sp, sp, -4
.end_macro

.macro Invert
	lw	t1, 4(sp)
	li	t0, 1
	beqz	t1, invert_exit
	li	t0, 0
invert_exit:
	sw	t0, 4(sp)
.end_macro

.macro JumpToElse $label_name
	#consume the value of the logical binary operation
	lw	t0, 4(sp)
	addi	sp, sp, 4
	#jumps to else label if the condition is zero
	beqz	t0, $label_name
.end_macro

.macro Minus
	Popt1t2
	sub	t0, t1, t2
	sw	t0, (sp)
	addi	sp, sp, -4
.end_macro

.macro Plus
	Popt1t2
	add	t0, t1, t2
	sw	t0, (sp)
	addi	sp, sp, -4
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

.macro    PopRel      $index
	#calculates offset
	li 	t0, 12
	li 	t1, 4
	li 	t2, $index
	mul 	t1, t1, t2
	add	t0, t0, t1
            
	#turns it to negative
	li	t1, -1
	mul	t0, t0, t1
            
	#applies the offset
	add	t0, t0, fp
            
	#retrives the value at the top of stack
	addi	sp, sp, 4
	lw    	t1, (sp)

	#saves the value to the var
	sw	t1, (t0)
.end_macro

.text
boot:	
	SetFP
	
	PrepareFunc 	0
	Invoke main
	Terminate

main:
	PushImm 10
	
while_body:
	PushVar	0
	Print
	Discard
	
	PushVar		0
	PushImm		1
	Minus
	PopRel		0
	Reserve
	Discard	
	
while_cond:
	PushVar	0
	PushImm	0
	CompGT	1
	JumpToElse	while_exit
	j 		while_body

while_exit:
	
	Return
	

