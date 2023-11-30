.data
    n: .word 101
    print_int_code: .word 1
    print_string_code: .word 4 
    exit_code: .word 10
    print_then: .string "then clause ran"
    print_else: .string "else clause ran"

.text

main:
    li t0, 101
    lw t1, n

then: 
	blt t1, t0, else #check condition, jump to else or stay in then
	lw a7, print_string_code
	la a0, print_then
	ecall
	j exit #jump to exit to skip else
	
else:
	lw a7, print_string_code
	la a0, print_else	
	ecall
	
exit:
    lw a7, exit_code
    ecall
