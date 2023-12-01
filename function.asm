.macro	Print
	lw            a7, print_str_code
        lw            a0, 4(sp)
        addi          sp, sp, 4
        ecall
.end_macro

.macro    Invoke      $address
        jal           next
    next:
        mv            t1, ra
        addi          t1, t1, 20
        sw            t1, (sp)
        addi          sp, sp, -4
        j             $address
.end_macro

.macro    PushImm     $number
        li            t1, $number
        sw            t1, (sp)
        addi          sp, sp, -4
.end_macro


.text
boot:	

	addi fp, fp, -4
	mv sp, fp
	
	#return value
	PushImm 0	
	#local variables
	PushImm 10
	PushImm 9
	Invoke main
	
	
	#sets sys call to print str and prints str
	lw a7, print_str_code
	la, a0, print_func
	ecall
	
	#sets sys call to print return value
	lw a7, print_int_code
	lw, a0, (fp)
	ecall
		

	lw a7, exit_code
	li a0, 0
	ecall

main:
	lw a7, print_int_code
	
	lw, a0, -4(fp)
	ecall
	
	lw, a0, -8(fp)
	ecall
	
	lw ra, 4(sp)
	jalr zero, ra, 0
	    

.data
    n: .word 100
    print_func: .string "terminated with: "    
    
    exit_code: .word 93
    print_int_code: .word 1
    print_str_code: .word 4
