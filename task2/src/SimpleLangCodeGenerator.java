import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class SimpleLangCodeGenerator extends AbstractParseTreeVisitor<String> implements SimpleLangVisitor<String>
{
    private static final String stackMachineMacros = """
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
            
            .macro PrintNewLine
                li              a7, 4
                la              a0, newline
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

        // return value
        sb.append("""
                .data
                    n: .word 100
                    terminate: .string "terminated with: "
                    print_func: .string "terminated func with: "
                    newline: .string "\\n"
                   
                    exit_code: .word 10
                    print_int_code: .word 1
                    print_str_code: .word 4
                        
                        
                        
                .text
                                
                # bootstrap loader that runs main()
                        
                boot:
                    SetFP
                    PushReturnValue     0       # return value
                        
                """);

        for (String s : args) {

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

                    if (arg > 0) {
                        sb.append(String.format("""
                                    PushImm     %d
                                """, arg)
                        );
                    } else {
                        sb.append(String.format("""
                                    PushImmNeg     %d
                                """, (arg * -1))
                        );
                    }


                } catch (NumberFormatException nfe) {
                    throw new RuntimeException(nfe);
                }

            }

        }

        sb.append("""
            Invoke      main
            lw a7, exit_code
            li a0, 0
            ecall
        """);

        for (int i = 0; i < ctx.dec().size(); ++i) {
            sb.append(visit(ctx.dec().get(i)));
        }

        return (stackMachineMacros + sb);
    }

    @Override public String visitProg(SimpleLangParser.ProgContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }
    @Override public String visitDec(SimpleLangParser.DecContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("""
        %s:
        """, ctx.typed_idfr(0).Idfr().getText())
        );

        for (int i = 0; i < ctx.vardec.size(); ++i) {
            String var_name = ctx.vardec.get(i).Idfr().getText();
            localVars.put(var_name, 12 + (i * 4));
        }

        sb.append(visit(ctx.body()));


        sb.append("""
            PopRel 0
            Discard 4
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
                    Discard 4
                """
                );
            }
        }

        return sb.toString();

    }
    public Integer getNextOffset() {
        Integer offset = null;
        for (int i = blockVars.size() - 1; i >= 0; i--) {
            Map<String, Integer> block = blockVars.get(i);

            if (block.isEmpty()) {
                continue;
            }

            int max = Collections.max(block.values());
            System.out.println("max: " + max);
            offset = Collections.max(block.values()) + 4;
            System.out.println("offset: " + offset );
            break;
        }

        if (offset == null) {
            offset = (localVars.isEmpty()) ? 12 : Collections.max(localVars.values()) + 4;
        }
        return offset;
    }


    @Override public String visitBlock(SimpleLangParser.BlockContext ctx)
    {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> map = new HashMap<>();

        boolean check = ctx.getParent().getClass() == SimpleLangParser.WhileExprContext.class;

        if (check) {
            int offset = getNextOffset();
            String RA_name = String.format("%d_RA",blockVars.size());
            map.put(RA_name, offset);
        }

        blockVars.add(map);


        for (int i = 0; i < ctx.ene.size(); ++i) {
            String output = visit(ctx.ene.get(i));

            if (i == ctx.ene.size() - 1 && output == null) {
                throw new RuntimeException("The last expression of a block or a body cannot end with \";\".");
            }


            sb.append(output);
            if (i != ctx.ene.size() - 1) {
                sb.append("""
                    Discard 4
                """
                );
            }
        }

        if (check) {
            Integer RA_address = Collections.min(map.values());
            Integer min_offset = RA_address + 8;
            sb.append(String.format("""
                    #loads the return address to ra
                    lw      ra, -%d(fp)
                
                    #resets the sp
                    addi sp, fp, -%d
                    
                    #jumps to cond
                    jalr    zero, ra, 0
                """, RA_address, min_offset
            ));

        } else {
            if (!map.isEmpty()) {
                Integer min_offset = Collections.min(map.values());
                Integer sp_address = min_offset + 4;
                sb.append(String.format("""
                        #loads value at the top of stack
                        lw t1, 4(sp)
                        #saves it to the rv of the block
                        sw t1, -%d(fp)
                        #resets the sp
                        addi sp, fp, -%d
                        """, min_offset, sp_address
                ));
            }

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


        Integer offset = (localVars.isEmpty()) ? 12 : Collections.max(localVars.values()) + 4;
        localVars.put(var_name, offset);


        sb.append("""
            Reserve 4
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
            Reserve 4
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
                    Reserve     4
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
                    PopRel      (%d)
                """, offset) +
                """
                    Reserve     4
                """;
    }
    @Override public String visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx)
    {

        StringBuilder sb = new StringBuilder();

        sb.append(visit(ctx.exp(0)));
        sb.append(visit(ctx.exp(1)));

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) {
            case SimpleLangParser.Eq -> sb.append("""
                CompEq
            """
            );

            case SimpleLangParser.Less -> sb.append("""
                CompGE
                Invert
            """
            );

            case SimpleLangParser.LessEq -> sb.append("""
                CompGT
                Invert
            """
            );

            case SimpleLangParser.Greater -> sb.append("""
                CompGT
            """
            );

            case SimpleLangParser.GreaterEq -> sb.append("""
                CompGE
            """
            );

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

        // return value
        sb.append("""
            PushReturnValue 0       # return value
        """);

        for (int i = 0; i < ctx.args.size(); i++) {
            sb.append(visit(ctx.args.get(i)));
        }

        sb.append(String.format("""
            Invoke      %s
        """, ctx.Idfr().getText())
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

        String elseLabel = String.format("else_label_%d", labelCounter++);
        String exitLabel = String.format("exit_label_%d", labelCounter++);

        sb.append(visit(ctx.exp()));


        sb.append(String.format("""
            Invert
            JumpTrue    %s
        """, elseLabel)
        );

        sb.append(visit(ctx.block(0)));

        sb.append(String.format("""
            Jump        %s
        """, exitLabel)
        );

        sb.append(String.format("""
        %s:
        """, elseLabel)
        );

        sb.append(visit(ctx.block(1)));

        sb.append(String.format("""
        %s:
        """, exitLabel)
        );

        return sb.toString();
    }

    @Override public String visitParenExpr(SimpleLangParser.ParenExprContext ctx) {
        return visit(ctx.exp());
    }

    @Override public String visitWhileExpr(SimpleLangParser.WhileExprContext ctx) {
        StringBuilder sb = new StringBuilder();

        String startLabel = String.format("start_label_%d", labelCounter++);
        String condLabel = String.format("cond_label_%d", labelCounter++);
        String blockLabel = String.format("block_label_%d", labelCounter++);
        String exitLabel = String.format("exit_label_%d", labelCounter++);

        sb.append(String.format("""
            jal %s
        %s:
            mv      t1, ra
            addi    t1, t1, 20
            sw      t1, (sp)
            addi    sp, sp, -4
            j %s
        %s:
        """,startLabel, startLabel, condLabel, condLabel
        ));

        sb.append(visit(ctx.exp()));
        sb.append(String.format("""
            JumpTrue    %s
            j           %s
            
        %s:
        """, blockLabel, exitLabel, blockLabel
        ));

        sb.append(visit(ctx.block()));

        sb.append(String.format("""
        %s:
            Discard     4
        """, exitLabel
        ));
        return sb.toString();
    }

    @Override public String visitRepeatExpr(SimpleLangParser.RepeatExprContext ctx) {
        return "";
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
            System.out.println("came into this clause");
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
            Reserve 4
        """);

        return sb.toString();
    }
    @Override public String visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx)
    {
        return """
                    Reserve     4
                """;
    }

    @Override public String visitNewLineExpr(SimpleLangParser.NewLineExprContext ctx) {
        return """
                    Reserve     4
                """;
    }
    @Override public String visitSkipExpr(SimpleLangParser.SkipExprContext ctx) {
        return """
                    Reserve     4
                """;
    }
    @Override public String visitIdExpr(SimpleLangParser.IdExprContext ctx)
    {
        if (!blockVars.isEmpty()) {
            return visitIdExprBlock(ctx);
        }

        String var_name = ctx.getText();
        Integer offset = localVars.get(var_name);

        if (offset == null) {
            throw new RuntimeException(String.format(
                    "Variable (int) : \"%s\" has not been initialized but is referenced", var_name
            ));
        }

        return String.format("""
                    PushRel     (%d)
                """, offset);
    }
    public String visitIdExprBlock(SimpleLangParser.IdExprContext ctx) {
        StringBuilder sb = new StringBuilder();
        String var_name = ctx.getText();
        Integer offset = null;


        System.out.println("size " + blockVars.size() + ": " + blockVars.get(blockVars.size() - 1));


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

        if (value > 0) {
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
