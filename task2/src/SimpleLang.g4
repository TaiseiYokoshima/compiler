grammar SimpleLang;

prog : dec+ EOF;

dec
    : typed_idfr LParen (vardec+=typed_idfr (Comma vardec+=typed_idfr)*)? RParen body
;


typed_idfr
    : type Idfr
;

type
    : IntType | BoolType | UnitType
;

body
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace
;

block
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace
;

exp
    : typed_idfr Assign exp                                 #AssignExpr
    | Idfr Assign exp                                       #ReassignExpr
    | LParen exp binop exp RParen                           #BinOpExpr
    | Idfr LParen (args+=exp (Comma args+=exp)*)? RParen    #InvokeExpr
    | block                                                 #BlockExpr
    | If exp Then block Else block                          #IfExpr
    | LParen exp RParen                                     #ParenExpr
    | While exp Do block                                    #WhileExpr
    | Repeat block Until exp                                #RepeatExpr
    | Print exp                                             #PrintExpr
    | Space                                                 #SpaceExpr
    | NewLine                                               #NewLineExpr
    | Skip                                                  #SkipExpr
    | Idfr                                                  #IdExpr
    | IntLit                                                #IntExpr
    | BoolLit                                               #BoolExpr
;

binop
    : Eq              #EqBinop
    | Less            #LessBinop
    | LessEq          #LessEqBinop
    | Greater         #GreaterBinop
    | GreaterEq       #GreaterEqBinop

    | And             #AndBinop
    | Or              #OrBinop
    | Xor             #XorBinop

    | Plus            #PlusBinop
    | Minus           #MinusBinop
    | Times           #TimesBinop
    | Divide          #DivideBinop
;

While       : 'while' ;
Do          : 'do';
Repeat      : 'repeat';
Until       : 'until';

LParen      : '(' ;
Comma       : ',' ;
RParen      : ')' ;
LBrace      : '{' ;
Semicolon   : ';' ;
RBrace      : '}' ;

Eq          : '==' ;
Less        : '<' ;
LessEq      : '<=' ;
Greater     : '>';
GreaterEq   : '>=';

And         : '&';
Or          :  '|';
Xor         : '^';

Plus        :  '+' ;
Times       : '*' ;
Minus       : '-' ;
Divide      : '/' ;

Assign      : ':=' ;

Print       : 'print' ;
Space       : 'space' ;
NewLine     : 'newline' ;
If          : 'if' ;
Then        : 'then' ;
Else        : 'else' ;

IntType     : 'int' ;
BoolType    : 'bool' ;
UnitType    : 'unit' ;

BoolLit     : 'true' | 'false' ;
IntLit      : '0' | ('-'? [1-9][0-9]*) ;
Skip        : 'skip' ;
Idfr        : [a-z][A-Za-z0-9_]* ;
WS          : [ \n\r\t]+ -> skip ;
