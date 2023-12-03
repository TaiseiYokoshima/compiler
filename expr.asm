.macro	Print
	lw            a7, print_str_code
        lw            a0, 4(sp)
        addi          sp, sp, 4
        ecall
.end_macro
.macro    SetFP
        mv            fp, sp
.end_macro

.macro    Invoke      $address 
	#saves jump address to ra
        jal           next
    next:
        mv            t1, ra
        addi          t1, t1, 16
        sw            t1, -8(fp)
        j             $address
.end_macro

.macro    PushReturnValue $int
       	#saves previous fp address	
	sw fp, -4(sp)
	
	#sets fp to new stack
	SetFP
	
	#pushes return Value
        PushImm $int

        #moves down sp to arg sec
        addi sp, sp, -8
.end_macro

.macro    Return
	#resets sp
	mv sp, fp
	#sets sp to one lower than the RV on the stack
	addi sp, sp, -4
	
	#load return address to ra
	lw ra, -8(fp)
	
	#load previous fp 
	lw fp, -4(fp)

	#exits by jumping to the address stored at ra
	jalr zero, ra, 0
.end_macro

.macro    PushImm     $number
        li            t1, $number
        sw            t1, (sp)
        addi          sp, sp, -4
.end_macro

.macro    PushImmNeg    $number
        li            t1, -$number
        sw            t1, (sp)
        addi          sp, sp, -4
.end_macro

.macro    PrintReturnValue  $funcname
	lw a7, print_str_code
	la a0, $funcname
	ecall
	      
	lw a7, print_int_code
	lw a0, 4(sp)
	ecall
	PrintNewline
.end_macro

.macro    PrintNewline  	
	lw a7, print_str_code
	la a0, newline
	ecall	
.end_macro

.macro    PrintInt  $th	
	mv t0, fp

	# t1:4 t2: $th
	# t1: 4 * $th
	li t1, 4
	li t2, $th
	mul t1, t1, t2
	
	#t1: 4 * $th t2: 16
	#t1: (4 * $th) + 16
	li t2, 12
	add t1, t1, t2
	
	#t2: -1
	#t1: -((4 * $th) + 16)
	li t2, -1
	mul t1, t1, t2
	
	#t0: fp -((4 * $th) + 16)
	add t0, t0, t1

	lw a7, print_int_code
	lw a0, (t0)
	ecall
	PrintNewline
.end_macro

.macro    Discard     $bytes
	addi	sp, sp, $bytes
.end_macro

.macro    Plus
        Popt1t2
        add           t1, t1, t2
        sw            t1, (sp)
        addi          sp, sp, -4
.end_macro

.macro    Popt1t2
        lw            t1, 4(sp)
        addi          sp, sp, 4
        lw            t2, 4(sp)
        addi          sp, sp, 4
.end_macro

.macro    Times
        Popt1t2
        mul           t1, t1, t2
        sw            t1, (sp)
        addi          sp, sp, -4
.end_macro


.text
boot:	
	#Pushes return value and calls the function
	PushReturnValue 0
	PushImm 10
	PushImm 9
	Invoke main 0
	
	#prints return value
	PrintReturnValue terminate
		
	lw a7, exit_code
	li a0, 0
	ecall
	
main:		
	#prints local variables
	PrintInt 0
	PrintInt 1
	
	PushImm 10
	PushImm 100
	Plus
	PushImm 2
	Times
	
	PrintInt 2
	Return

func:
	PushImm 50
	PrintInt 0
	Return



.data
    n: .word 100
    terminate: .string "terminated with: "    
    print_func: .string "terminated func with: "
    newline: .string "\n"
    
    exit_code: .word 10
    print_int_code: .word 1
    print_str_code: .word 4
