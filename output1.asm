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

.macro    Reserve     $bytes
    addi          sp, sp, -$bytes
.end_macro

.macro    Discard     $bytes
    addi          sp, sp, $bytes
.end_macro

.macro    SetFP
    mv            fp, sp
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

.macro    SaveFP
    sw            fp, (sp)
    addi          sp, sp, -4
.end_macro

.macro    RestoreFP
    lw            fp, 4(sp)
    addi          sp, sp, 4
.end_macro

.macro    Popt1t2
    lw            t2, 4(sp)
    addi          sp, sp, 4
    lw            t1, 4(sp)
    addi          sp, sp, 4
.end_macro

.macro    CompGT
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    bgt           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro    CompGE
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    bge           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro    CompEq
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    beq           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro    Invert
    lw            t1, 4(sp)
    li            t0, 1
    sw            t0, 4(sp)
    beqz          t1, exit
    sw            zero, 4(sp)
exit:
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

.macro    Plus
    Popt1t2
    add           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    Minus
    Popt1t2
    sub           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
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

.macro    Invoke      $address
	#saves jump address to ra
    jal           next
    next:
        mv            t1, ra
        addi          t1, t1, 16
        sw            t1, -8(fp)
        j             $address
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

.macro    Print
    li            a7, 1
    lw            a0, 4(sp)
    addi          sp, sp, 4
    ecall
.end_macro

.macro    PrintSpace
    li            a7, 11
    li            a0, 32
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



.text

# bootstrap loader that runs main()
boot:
    SetFP
    PushReturnValue     0       # return value

    Invoke      main
    lw a7, exit_code
    li a0, 0
    ecall
main:
    PushImmNeg     0
    Reserve 4
    Discard 4
    #print expression
    PushRel     (12)
    Print
    Reserve 4
    Discard 4
    PushImm 1
    Invert
    JumpTrue    else_label_0
    PushImmNeg     10
    Reserve 4
lw t1, 4(sp)
sw t1, -16(fp)
addi sp, fp, -20
    Jump        exit_label_1
else_label_0:
    Reserve 4
exit_label_1:
    Discard 4
    #print expression
    PushRel     (null)
    Print
    Reserve 4
    PopRel 0
    Discard 4
    Return
    