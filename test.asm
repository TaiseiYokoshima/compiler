
.text
boot:
	li, t1, 999
	sw, t1, -4(fp)
	
	lw, a7, print_int_code
	mv, a0, t1
	ecall
	
	


.data
    n: .word 100
    print_func: .string "return value is: "    
    
    exit_code: .word 93
    print_int_code: .word 1
    print_str_code: .word 4
    print_newline_code: .word
