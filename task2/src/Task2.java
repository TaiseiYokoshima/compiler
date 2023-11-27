import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import rars.AssemblyException;
import rars.SimulationException;
import rars.api.Options;
import rars.api.Program;

import java.io.IOException;

public class Task2 {


    public static void main(String[] args) throws IOException {
        // create a CharStream that reads from standard input
        CharStream input = CharStreams.fromStream(System.in);

        // create a lexer that feeds off of input CharStream
        SimpleLangLexer lexer = new SimpleLangLexer(input);

        // create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // create a parser that feeds off the tokens buffer
        SimpleLangParser parser = new SimpleLangParser(tokens);
        SimpleLangParser.ProgContext tree = parser.prog(); // begin parsing at prog rule


        SimpleLangCodeGenerator codegen = new SimpleLangCodeGenerator();
        String generatedCode = codegen.visitProgram(tree, args);

        System.out.println(generatedCode);

        Options op = new Options();
        op.maxSteps = 1000000000;
        Program p = new Program(op);

        try {
            p.assembleString(generatedCode);
        } catch (AssemblyException ae){
            throw new RuntimeException(ae);
        }

        p.setup(null, "");

        String terminationReason;
        try {
            terminationReason = p.simulate().toString();
        } catch (SimulationException se){
            throw new RuntimeException(se);
        }

        System.out.println(p.getSTDOUT());
        System.out.println(terminationReason);
        System.out.println(p.getRegisterValue("a0"));

    }
}