grammar SimpleLang;

prog : dec+ EOF;


//dec
//    : typed_idfr LParen (vardec+=typed_idfr)? RParen body
//;

//updated
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
    : Idfr Assign exp                                       #ReassignExpr
    | typed_idfr Assign exp                                 #AssignExpr
    | LParen exp binop exp RParen                           #BinOpExpr
    | Idfr LParen (args+=exp (Comma args+=exp)*)? RParen    #InvokeExpr
    | block                                                 #BlockExpr
    | If exp Then block Else block                          #IfExpr
    | LParen exp RParen                                     #ParenExpr
    | While exp Do body                                     #WhileExpr
    | Repeat body Until exp                                 #RepeatExpr
    | Print exp                                             #PrintExpr
    | Space                                                 #SpaceExpr
    | NewLine                                               #NewLineExpr
    | Idfr                                                  #IdExpr
    | IntLit                                                #IntExpr
    | BoolLit                                               #BoolExpr
;

//added
//condition
//    : exp Or condition      #OrCondition
//
//    | exp Xor condition     #XorCondition
//    | exp                   #ConditionOperand
//;
//
//term
//    : exp And condition     #AndCondition
//
//;



binop
    : Eq              #EqBinop
    | Less            #LessBinop
    | LessEq          #LessEqBinop
    //added
    | Greater         #GreaterBinop
    | GreaterEq       #GreaterEqBinop

    //added
    | And             #AndBinop
    | Or              #OrBinop
    | Xor             #XorBinop

    | Plus            #PlusBinop
    | Minus           #MinusBinop
    | Times           #TimesBinop
    //added
    | Divide          #DivideBinop
;


//added
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
//added
Greater     : '>';
GreaterEq   : '>=';

//added
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
Idfr        : [a-z][A-Za-z0-9_]* ;
WS          : [ \n\r\t]+ -> skip ;