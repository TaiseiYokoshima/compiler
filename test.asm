
.data
    n: .word 100
    print_int_code: .word 1
    exit_code: .word 93

.text

main:
    lw a0, n

loop_start: 
    lw a7, print_int_code
    ecall
    addi a0, a0, -1
    bnez a0, loop_start

exit:
    lw a7, exit_code
    ecall
