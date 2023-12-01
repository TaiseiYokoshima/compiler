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
	addi fp, fp, -4
	mv sp, fp
	
	#return value
	PushImm 0	
	#local variables
	PushImm 10
	PushImm 9
	Invoke main
	PrintReturnValue terminate
		

	lw a7, exit_code
	li a0, 0
	ecall
	
main:
	PrintInt 4
	PrintInt 8
	
	mv fp, sp
	
	PushImm 70
	PushImm 50
	Invoke func
	PrintReturnValue print_func	

	lw ra, 4(sp)
	#set sp address to return value address
	mv sp, fp
	jalr zero, ra, 0
	    

func:
	PrintInt 4
	
	#load return address to ra
	lw ra, 4(sp)	
	
	#set sp address to return value address
	mv sp, fp
		
	#move back the fp 
	addi fp, fp, 16

	
	#return out - jump back
	jalr zero, ra, 0



.data
    n: .word 100
    terminate: .string "terminated with: "    
    print_func: .string "terminated func with: "
    newline: .string "\n"
    
    exit_code: .word 93
    print_int_code: .word 1
    print_str_code: .word 4
