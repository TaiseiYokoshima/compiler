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
	
	#load return adress to ra
	lw ra, -8(fp)
	
	#load previous fp 
	lw, fp, -4(fp)

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
	lw, a7, print_str_code
	la, a0, $funcname
	ecall
	      
	lw, a7, print_int_code
	lw, a0, (sp)
	ecall
	PrintNewline

.end_macro

.macro    PrintNewline  	
	lw, a7, print_str_code
	la, a0, newline
	ecall	
.end_macro

.macro    PrintInt  $offset	
	lw a7, print_int_code
	lw, a0, -$offset(fp)
	ecall
	PrintNewline
.end_macro


.text
boot:	
	#Pushes return value
	PushReturnValue 0
	
	Invoke main
	
	#prints return value
	PrintReturnValue terminate
		
	lw a7, exit_code
	li a0, 0
	ecall
	
main:	
	#local variables
	PushImmNeg 10
	PushImm 9
	
	#prints local variables
	PrintInt 12
	PrintInt 16

	
	#pushes return value
	PushReturnValue 70
	Invoke func
	
	#prints return value
	PrintReturnValue print_func	
	
	Return
	    

func:
	PushImm 50
	PrintInt 12
	Return



.data
    n: .word 100
    terminate: .string "terminated with: "    
    print_func: .string "terminated func with: "
    newline: .string "\n"
    
    exit_code: .word 10
    print_int_code: .word 1
    print_str_code: .word 4
