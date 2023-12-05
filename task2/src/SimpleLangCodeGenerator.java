import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;




public class SimpleLangCodeGenerator extends AbstractParseTreeVisitor<String> implements SimpleLangVisitor<String>
{
    private static final String stackMachineMacros = """
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
            
            .macro PushVarOffset    $offset
                lw      t0, -$offset(fp)
                sw      t0, (sp)
                addi    sp, sp, -4
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
                li	t0, 0
                beq	t1, t2, compeq_exit$s
                li	t0, 1
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
                        
            .macro JumpToElse $label_name
            	#consume the value of the logical binary operation
            	lw	t0, 4(sp)
            	addi	sp, sp, 4
            	#jumps to else label if the condition is zero
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
            	PrintNewLine
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
            """;

    // This records the offset of each parameter: fp + n
    private final Map<String, Integer> localVars = new HashMap<>();


    // keeps track of variables initialized within blocks
    private final ArrayList<Map<String, Integer>> blockVars = new ArrayList<>();

    // For simplicity, we will just use labels of the form "label_[some integer]"
    private int labelCounter = 0;

    public String visitProgram(SimpleLangParser.ProgContext ctx, String[] args)
    {
        StringBuilder sb = new StringBuilder();
        String os_name = System.getProperty("os.name").toLowerCase();

        if(os_name.contains("win")) {
            sb.append("""
                .data
                    n: .word 100
                    terminate: .string "terminated with: "
                    print_func: .string "terminated func with: "
                    newline: .string "\\n"
                   
                    exit_code: .word 93
                    print_int_code: .word 1
                    print_str_code: .word 4
                """);
        } else if(os_name.contains("nux") || os_name.contains("nix")) {
            sb.append("""
                .data
                    n: .word 100
                    terminate: .string "terminated with: "
                    print_func: .string "terminated func with: "
                    newline: .string "\\n"
                   
                    exit_code: .word 10
                    print_int_code: .word 1
                    print_str_code: .word 4
                """);
        }

        sb.append("""
            
            # code below:
            .text
                # bootstrap loader that runs main()
            boot:
                SetFP
                
            """);





        for(int i = 0; i < args.length; i++) {
            if (i == 0) {
                sb.append("""
                            ArgSetStart
                        """);
            }

            String s = args[i];
            if (s.equals("true")) {
                sb.append("""
                            PushImm     1
                        """);
            } else if (s.equals("false")) {
                sb.append("""
                            PushImm     0
                        """);
            } else {
                try {
                    int arg = Integer.parseInt(s);
                    sb.append(pushInt(arg));
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException(nfe);
                }
            }

            if (i == args.length - 1) {
                sb.append(String.format("""
                    ArgSetEnd     %d
                """, args.length
                ));
            }
        }

        sb.append(String.format("""
            PrepareFunc     %d
            Invoke      main
            Terminate
        """, args.length
        ));

        for (int i = 0; i < ctx.dec().size(); ++i) {
            sb.append(visit(ctx.dec().get(i)));
        }
        return (stackMachineMacros + sb);
    }
    public String pushInt(int value) {
        StringBuilder sb = new StringBuilder();
        if (value > 0 || value == 0) {
            sb.append(String.format("""
                PushImm     %d
            """, value)
            );
        } else {
            sb.append(String.format("""
                PushImmNeg     %d
            """, (value * -1))
            );
        }
        return sb.toString();
    }

    @Override public String visitProg(SimpleLangParser.ProgContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitDec(SimpleLangParser.DecContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
        %s:
        """, ctx.typed_idfr(0).Idfr().getText())
        );

        for (int i = 0; i < ctx.vardec.size(); ++i) {
            String var_name = ctx.vardec.get(i).Idfr().getText();
            localVars.put(var_name, i);
        }

        sb.append(visit(ctx.body()));


        sb.append("""
            Return
        """);

        localVars.clear();

        return sb.toString();
    }
    @Override public String visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public String visitType(SimpleLangParser.TypeContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public String visitBody(SimpleLangParser.BodyContext ctx)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ctx.ene.size(); ++i) {

            String output = visit(ctx.ene.get(i));

            if (i == ctx.ene.size() - 1 && output == null ) {
                throw new RuntimeException("The last expression of a block or a body cannot end with \";\".");
            }
            sb.append(output);
            if (i != ctx.ene.size() - 1) {
                sb.append("""
                    Discard
                """
                );
            }
        }

        return sb.toString();
    }
    public Integer getNextOffset() {
        Integer index = null;
        for (int i = blockVars.size() - 1; i >= 0; i--) {
            Map<String, Integer> block = blockVars.get(i);

            if (block.isEmpty()) {
                continue;
            }

            index = Collections.max(block.values()) + 1;
            break;
        }

        if (index == null) {
            index = (localVars.isEmpty()) ? 0 : Collections.max(localVars.values()) + 1;
        }
        return index;
    }


    @Override public String visitBlock(SimpleLangParser.BlockContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> map = new HashMap<>();
        blockVars.add(map);

        for (int i = 0; i < ctx.ene.size(); ++i) {
            String output = visit(ctx.ene.get(i));
            if (i == ctx.ene.size() - 1 && output == null) {
                throw new RuntimeException("The last expression of a block or a body cannot end with \";\".");
            }

            sb.append(output);
            if (i != ctx.ene.size() - 1) {
                sb.append("""
                    Discard
                """
                );
            }
        }

        if (!map.isEmpty()) {
            int sp_address = 4 * map.size();
            int RV_address = sp_address + 4;
            sb.append(String.format("""
                    #loads value at the top of stack (return value)
                    lw t1, 4(sp)
                    #saves return value to the RV of the block
                    sw t1, %d(sp)
                    #resets sp
                    addi sp, sp, %d
                """, RV_address, sp_address
            ));
        }
        blockVars.remove(blockVars.size() - 1);
        return sb.toString();
    }

    @Override public String visitAssignExpr(SimpleLangParser.AssignExprContext ctx)
    {
        if (!blockVars.isEmpty()) {
            return block_variable_initialization(ctx);
        }

        String var_name = ctx.typed_idfr().Idfr().getText();
        if (localVars.get(var_name) != null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has already been initialized but is re-initialized", var_name
            ));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.exp()));

        Integer index = (localVars.isEmpty()) ? 0 : Collections.max(localVars.values()) + 1;
        localVars.put(var_name, index);

        sb.append("""
            Reserve
        """);
        return sb.toString();
    }
    public String block_variable_initialization(SimpleLangParser.AssignExprContext ctx) {
        String var_name = ctx.typed_idfr().Idfr().getText();
        Map<String, Integer> this_block = blockVars.get(blockVars.size() -1);
        if (this_block.get(var_name) != null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has already been initialized but is re-initialized", var_name
            ));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.exp()));

        Integer offset = getNextOffset();
        this_block.put(var_name, offset);
        sb.append("""
            Reserve
        """);
        return sb.toString();

    }

    @Override public String visitReassignExpr(SimpleLangParser.ReassignExprContext ctx)
    {
        if (!blockVars.isEmpty()) {
            return block_variable_reassign(ctx);
        }

        String var_name = ctx.Idfr().getText();

        Integer offset = localVars.get(var_name);

        if (offset == null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has not been initialized but is reassigned", var_name
            ));
        }

        return visit(ctx.exp()) +
                String.format("""
                    PopRel      (%d)
                """, offset) +
                """
                    Reserve
                """;
    }

    public String block_variable_reassign(SimpleLangParser.ReassignExprContext ctx) {
        String var_name = ctx.Idfr().getText();

        Integer offset = null;
        for (int i = blockVars.size() - 1; i >= 0; i--) {
            Map<String, Integer> block = blockVars.get(i);
            offset = block.get(var_name);
            if (offset != null) {
                break;
            }
        }

        if (offset == null) {
            offset = localVars.get(var_name);
        }

        if (offset == null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has not been initialized but is reassigned", var_name
            ));
        }

        return visit(ctx.exp()) +
                String.format("""
                    PopRel      %d
                """, offset) +
                """
                    Reserve
                """;
    }
    @Override public String visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.exp(0)));
        sb.append(visit(ctx.exp(1)));

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {
            case SimpleLangParser.Eq -> sb.append(String.format("""
                CompEq      %d
            """, labelCounter++
            ));

            case SimpleLangParser.Less -> sb.append(String.format("""
                CompGE      %d
                Invert      %d
            """, labelCounter++, labelCounter++
            ));

            case SimpleLangParser.LessEq -> sb.append(String.format("""
                CompGT      %d
                Invert      %d
            """, labelCounter++, labelCounter++
            ));

            case SimpleLangParser.Greater -> sb.append(String.format("""
                CompGT      %d
            """, labelCounter++
            ));

            case SimpleLangParser.GreaterEq -> sb.append(String.format("""
                CompGE      %d
            """, labelCounter++
            ));

            case SimpleLangParser.And -> sb.append("""
                AndBinop
            """);

            case SimpleLangParser.Or -> sb.append("""
                OrBinop
            """);

            case SimpleLangParser.Xor -> sb.append("""
                XorBinop
            """);

            case SimpleLangParser.Plus -> sb.append("""
                Plus
            """
            );

            case SimpleLangParser.Minus -> sb.append("""
                Minus
            """
            );

            case SimpleLangParser.Times -> sb.append("""
                Times
            """
            );

            case SimpleLangParser.Divide -> sb.append("""
                Divide
            """);

            default -> throw new RuntimeException("Shouldn't be here - wrong binary operator.");
        }
        return sb.toString();
    }

    @Override public String visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        int num_args = ctx.args.size();

        for (int i = 0; i < ctx.args.size(); i++) {
            if (i == 0) {
                sb.append("""
                    ArgSetStart
                """);
            }

            sb.append(visit(ctx.args.get(i)));

            if (i == ctx.args.size() - 1) {
                sb.append(String.format("""
                    ArgSetEnd %d
                """,num_args
                ));
            }
        }

        sb.append(String.format("""
            PrepareFunc %d
            Invoke      %s
        """,num_args, ctx.Idfr().getText())
        );

        return sb.toString();

    }

    @Override public String visitBlockExpr(SimpleLangParser.BlockExprContext ctx)
    {
        return visit(ctx.block());
    }

    @Override public String visitIfExpr(SimpleLangParser.IfExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();

        String condLabel = String.format("cond_label_%d", labelCounter++);
        String thenLabel = String.format("then_label_%d", labelCounter++);
        String elseLabel = String.format("else_label_%d", labelCounter++);
        String end_ifLabel = String.format("end_if_label_%d", labelCounter++);


        sb.append(String.format("""
        %s:
        """, condLabel
        ));
//        pushes output of conditional binop to stack
        sb.append(visit(ctx.exp()));

//        inverts boolean on stack and jumps to else if condition was a 0
        sb.append(String.format("""
            JumpToElse    %s
        
        %s:
        """, elseLabel, thenLabel)
        );

//        then block expressions pasted here
        sb.append(visit(ctx.block(0)));

        sb.append(String.format("""
            j        %s
        """, end_ifLabel)
        );

        sb.append(String.format("""
        %s:
        """, elseLabel)
        );

        sb.append(visit(ctx.block(1)));

        sb.append(String.format("""
        %s:
        """, end_ifLabel)
        );

        return sb.toString();
    }

    @Override public String visitParenExpr(SimpleLangParser.ParenExprContext ctx) {
        return visit(ctx.exp());
    }

    @Override public String visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {
        StringBuilder sb = new StringBuilder();

        String condLabel = String.format("cond_label_%d", labelCounter++);
        String blockLabel = String.format("block_label_%d", labelCounter++);
        String exitLabel = String.format("exit_label_%d", labelCounter++);

//        cond label
        sb.append(String.format("""
        %s:
        """, condLabel
        ));

//        condition check expr
        sb.append(visit(ctx.exp()));

//        jumping based on condition
        sb.append(String.format("""
            Invert
            lw      t1, 4(sp)
            addi    sp, sp, 4
            bnez    t1, %s
        """, exitLabel
        ));

//        block label
        sb.append(String.format("""
        %s:
        """, blockLabel
        ));

//        block expressions
        sb.append(visit(ctx.block()));

        sb.append(String.format("""
            j   %s
        %s:
        """, condLabel, exitLabel
        ));
        return sb.toString();
    }

    @Override public String visitRepeatExpr(SimpleLangParser.RepeatExprContext ctx) {
        StringBuilder sb = new StringBuilder();
        String blockLabel = String.format("block_label_%d", labelCounter++);
        String condLabel = String.format("cond_label_%d", labelCounter++);
        String exitLabel = String.format("exit_label_%d", labelCounter++);

        sb.append(String.format("""
        %s:
        """, blockLabel
        ));

        sb.append(visit(ctx.block()));

        sb.append(String.format("""
        %s:
        """, condLabel
        ));

        sb.append(visit(ctx.exp()));


        sb.append(String.format("""
            Invert
            lw      t1, 4(sp)
            addi    sp, sp, 4
            bnez    t1, %s
        """, blockLabel
        ));

        sb.append(String.format("""
        %s:
        """, exitLabel
        ));
        return sb.toString();
    }

    @Override public String visitPrintExpr(SimpleLangParser.PrintExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();
        sb.append("""
            #print expression
        """);

        if (ctx.exp().getClass() == SimpleLangParser.SpaceExprContext.class) {
            sb.append("""
                        PrintSpace
                    """
            );
        } else if (ctx.exp().getClass() == SimpleLangParser.NewLineExprContext.class) {
            sb.append("""
                PrintNewLine
            """);

        } else {
            sb.append(visit(ctx.exp()));
            sb.append("""
                Print
            """
            );
        }

        sb.append("""
            Reserve
        """);

        return sb.toString();
    }
    @Override public String visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx) {
        return """
                    Reserve
                """;
    }

    @Override public String visitNewLineExpr(SimpleLangParser.NewLineExprContext ctx) {
        return """
                    Reserve
                """;
    }
    @Override public String visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        return """
                    Reserve
                """;
    }
    @Override public String visitIdExpr(SimpleLangParser.IdExprContext ctx)
    {
//        if (!blockVars.isEmpty()) {
//            return visitIdExprBlock(ctx);
//        }

        String var_name = ctx.getText();
        Integer index = localVars.get(var_name);

        if (index == null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has not been initialized but is referenced", var_name
            ));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                    PushVar     %d
                """, index));
        return sb.toString();
    }
    public String visitIdExprBlock(SimpleLangParser.IdExprContext ctx) {
        StringBuilder sb = new StringBuilder();
        String var_name = ctx.getText();
        Integer offset = null;



        for (int i = blockVars.size() - 1; i >= 0; i--) {
            Map<String, Integer> block = blockVars.get(i);
            offset = block.get(var_name);
            if (offset != null) {
                break;
            }
        }

        if (offset == null) {
            offset = localVars.get(var_name);
        }

        if (offset == null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has not been initialized but is referenced", var_name
            ));
        }


        sb.append(String.format("""
            PushRel     (%d)
        """, offset)
        );

        return sb.toString();
    }

    @Override public String visitIntExpr(SimpleLangParser.IntExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        int value = Integer.parseInt(ctx.IntLit().getText());

        if (value > 0 || value == 0) {
            sb.append(String.format("""
                PushImm     %d
            """, value)
            );
        } else {
            sb.append(String.format("""
                PushImmNeg     %d
            """, value * -1)
            );
        }
        return sb.toString();
    }
    @Override public String visitBoolExpr(SimpleLangParser.BoolExprContext ctx)
    {
        StringBuilder sb = new StringBuilder();

        if (ctx.BoolLit().getText().equals("true")) {
            sb.append("""
                PushImm 1
            """);
        } else {
            sb.append("""
                PushImm 0
            """);
        }
        return sb.toString();
    }

    @Override public String visitEqBinop(SimpleLangParser.EqBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitLessBinop(SimpleLangParser.LessBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitGreaterBinop(SimpleLangParser.GreaterBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitGreaterEqBinop(SimpleLangParser.GreaterEqBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitAndBinop(SimpleLangParser.AndBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitOrBinop(SimpleLangParser.OrBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitXorBinop(SimpleLangParser.XorBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitPlusBinop(SimpleLangParser.PlusBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitMinusBinop(SimpleLangParser.MinusBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitTimesBinop(SimpleLangParser.TimesBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitDivideBinop(SimpleLangParser.DivideBinopContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
}
