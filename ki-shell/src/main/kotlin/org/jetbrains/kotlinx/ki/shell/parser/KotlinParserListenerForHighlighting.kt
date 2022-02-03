// Generated from KotlinParser.g4 by ANTLR 4.8
package org.jetbrains.kotlinx.ki.shell.parser

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlinx.ki.shell.parser.KotlinParser.*

/**
 * Magic constants are taken from official grammar.
 * https://kotlinlang.org/docs/reference/grammar.html#ifExpression
 * https://kotlinlang.org/docs/reference/grammar.html#forStatement
 * https://kotlinlang.org/docs/reference/grammar.html#whileStatement
 * https://kotlinlang.org/docs/reference/grammar.html#doWhileStatement
 * The goal is navigating to the sub-elements not directly represented in it and
 * granular control over keywords space to be taken into account by highlighting.
 */
private const val IF_TOKEN_LENGTH = 2
private const val ELSE_TOKEN_LENGTH = 4
private const val FOR_TOKEN_LENGTH = 3
private const val WHILE_TOKEN_LENGTH = 5

/**
 * This class provides an empty implementation of [KotlinParserListener],
 * which can be extended to create a listener which only needs to handle a subset
 * of the available methods.
 */
class KotlinParserListenerForHighlighting : KotlinParserListener {

    enum class RecogizedElements {
        Unknown,
        Comment,
        Keyword,
        FunctionIdentifier,
        Identifier,
        TypeIdentifier,
        String,
        StringTemplate,
        Number,
        Parenthesis,
        TypeParameter,
        Label
    }

    data class ElementWithPos(
            val start: Int,
            val end: Int,
            val element: RecogizedElements
    )

    val result = mutableListOf<ElementWithPos>()

    var lastElement: RecogizedElements? = null
    var lastIdentifierTypeElement: RecogizedElements? = null
    var lastElementStart: Int? = null

    fun addHighlightingIdentifier(ctx: ParserRuleContext) {
        exitHighlightingElement(ctx)
        result.add(ElementWithPos(ctx.start.startIndex, ctx.stop.stopIndex, lastIdentifierTypeElement ?: RecogizedElements.Identifier))
        lastIdentifierTypeElement = null
    }

    fun enterIdentifierHighlighting(identifierElement: RecogizedElements) {
        lastIdentifierTypeElement = identifierElement
    }

    fun addHighlightingElement(ctx: ParserRuleContext, element: RecogizedElements) {
        exitHighlightingElement(ctx)
        result.add(ElementWithPos(ctx.start.startIndex, ctx.stop.stopIndex, element))
    }

    fun addHighlightingParenthesis(ctx: ParserRuleContext) {
        exitHighlightingElement(ctx)
        result.add(ElementWithPos(ctx.start.startIndex, ctx.start.startIndex, RecogizedElements.Parenthesis))
        result.add(ElementWithPos(ctx.stop.stopIndex, ctx.stop.stopIndex, RecogizedElements.Parenthesis))
    }

    fun enterHighlightingElement(ctx: ParserRuleContext, element: RecogizedElements) {
        exitHighlightingElement(ctx)
        lastElement = element
        lastElementStart = ctx.start.startIndex
    }
    
    fun enterHighlightingKeyword(ctx: ParserRuleContext) {
        enterHighlightingElement(ctx, RecogizedElements.Keyword)
    }
    
    fun cancelHighlightingKeyword() {
        lastElement = null
    }

    fun exitHighlightingElement(ctx: ParserRuleContext) {
        if (lastElement != null) {
            result.add(ElementWithPos(lastElementStart!!, ctx.start.startIndex - 1, lastElement!!))
            lastElement = null
        }
    }

    fun highlightKeywordWithOffset(ctx: ParserRuleContext, start: Int, tokenLength: Int) {
        exitHighlightingElement(ctx)
        result.add(ElementWithPos(start, start + tokenLength - 1, RecogizedElements.Keyword))
        lastElementStart = start
    }

    override fun enterKotlinFile(ctx: KotlinFileContext) {}

    override fun exitKotlinFile(ctx: KotlinFileContext) {}

    override fun enterScript(ctx: ScriptContext) {}

    override fun exitScript(ctx: ScriptContext) {}

    override fun enterShebangLine(ctx: ShebangLineContext) {}

    override fun exitShebangLine(ctx: ShebangLineContext) {
        addHighlightingElement(ctx, RecogizedElements.Comment)
    }

    override fun enterFileAnnotation(ctx: FileAnnotationContext) {}

    override fun exitFileAnnotation(ctx: FileAnnotationContext) {}

    override fun enterPackageHeader(ctx: PackageHeaderContext) {}

    override fun exitPackageHeader(ctx: PackageHeaderContext) {}

    override fun enterImportList(ctx: ImportListContext) {}

    override fun exitImportList(ctx: ImportListContext) {}

    override fun enterImportHeader(ctx: ImportHeaderContext) {}

    override fun exitImportHeader(ctx: ImportHeaderContext) {}

    override fun enterImportAlias(ctx: ImportAliasContext) {}

    override fun exitImportAlias(ctx: ImportAliasContext) {}

    override fun enterTopLevelObject(ctx: TopLevelObjectContext) {}

    override fun exitTopLevelObject(ctx: TopLevelObjectContext) {}

    override fun enterTypeAlias(ctx: TypeAliasContext) {}

    override fun exitTypeAlias(ctx: TypeAliasContext) {}

    override fun enterDeclaration(ctx: DeclarationContext) {}

    override fun exitDeclaration(ctx: DeclarationContext) {}

    override fun enterClassDeclaration(ctx: ClassDeclarationContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitClassDeclaration(ctx: ClassDeclarationContext) {}

    override fun enterPrimaryConstructor(ctx: PrimaryConstructorContext) {}

    override fun exitPrimaryConstructor(ctx: PrimaryConstructorContext) {}

    override fun enterClassBody(ctx: ClassBodyContext) {}

    override fun exitClassBody(ctx: ClassBodyContext) {}

    override fun enterClassParameters(ctx: ClassParametersContext) {}

    override fun exitClassParameters(ctx: ClassParametersContext) {}

    override fun enterClassParameter(ctx: ClassParameterContext) {}

    override fun exitClassParameter(ctx: ClassParameterContext) {}

    override fun enterDelegationSpecifiers(ctx: DelegationSpecifiersContext) {}

    override fun exitDelegationSpecifiers(ctx: DelegationSpecifiersContext) {}

    override fun enterDelegationSpecifier(ctx: DelegationSpecifierContext) {}

    override fun exitDelegationSpecifier(ctx: DelegationSpecifierContext) {}

    override fun enterConstructorInvocation(ctx: ConstructorInvocationContext) {}

    override fun exitConstructorInvocation(ctx: ConstructorInvocationContext) {}

    override fun enterAnnotatedDelegationSpecifier(ctx: AnnotatedDelegationSpecifierContext) {}

    override fun exitAnnotatedDelegationSpecifier(ctx: AnnotatedDelegationSpecifierContext) {}

    override fun enterExplicitDelegation(ctx: ExplicitDelegationContext) {}

    override fun exitExplicitDelegation(ctx: ExplicitDelegationContext) {}

    override fun enterTypeParameters(ctx: TypeParametersContext) {}

    override fun exitTypeParameters(ctx: TypeParametersContext) {}

    override fun enterTypeParameter(ctx: TypeParameterContext) {
        enterIdentifierHighlighting(RecogizedElements.TypeParameter)
    }

    override fun exitTypeParameter(ctx: TypeParameterContext) {}

    override fun enterTypeConstraints(ctx: TypeConstraintsContext) {}

    override fun exitTypeConstraints(ctx: TypeConstraintsContext) {}

    override fun enterTypeConstraint(ctx: TypeConstraintContext) {}

    override fun exitTypeConstraint(ctx: TypeConstraintContext) {}

    override fun enterClassMemberDeclarations(ctx: ClassMemberDeclarationsContext) {}

    override fun exitClassMemberDeclarations(ctx: ClassMemberDeclarationsContext) {}

    override fun enterClassMemberDeclaration(ctx: ClassMemberDeclarationContext) {}

    override fun exitClassMemberDeclaration(ctx: ClassMemberDeclarationContext) {}

    override fun enterAnonymousInitializer(ctx: AnonymousInitializerContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitAnonymousInitializer(ctx: AnonymousInitializerContext) {}

    override fun enterCompanionObject(ctx: CompanionObjectContext) {
        enterHighlightingKeyword(ctx)
        enterIdentifierHighlighting(RecogizedElements.TypeIdentifier)
    }

    override fun exitCompanionObject(ctx: CompanionObjectContext) {}

    override fun enterFunctionValueParameters(ctx: FunctionValueParametersContext) {}

    override fun exitFunctionValueParameters(ctx: FunctionValueParametersContext) {}

    override fun enterFunctionValueParameter(ctx: FunctionValueParameterContext) {}

    override fun exitFunctionValueParameter(ctx: FunctionValueParameterContext) {}

    override fun enterFunctionDeclaration(ctx: FunctionDeclarationContext) {
        enterHighlightingKeyword(ctx)
        enterIdentifierHighlighting(RecogizedElements.FunctionIdentifier)
    }

    override fun exitFunctionDeclaration(ctx: FunctionDeclarationContext) {}

    override fun enterFunctionBody(ctx: FunctionBodyContext) {}

    override fun exitFunctionBody(ctx: FunctionBodyContext) {}

    override fun enterVariableDeclaration(ctx: VariableDeclarationContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitVariableDeclaration(ctx: VariableDeclarationContext) {}

    override fun enterMultiVariableDeclaration(ctx: MultiVariableDeclarationContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitMultiVariableDeclaration(ctx: MultiVariableDeclarationContext) {}

    override fun enterPropertyDeclaration(ctx: PropertyDeclarationContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitPropertyDeclaration(ctx: PropertyDeclarationContext) {}

    override fun enterPropertyDelegate(ctx: PropertyDelegateContext) {}

    override fun exitPropertyDelegate(ctx: PropertyDelegateContext) {}

    override fun enterGetter(ctx: GetterContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitGetter(ctx: GetterContext) {}

    override fun enterSetter(ctx: SetterContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitSetter(ctx: SetterContext) {}

    override fun enterParametersWithOptionalType(ctx: ParametersWithOptionalTypeContext) {}

    override fun exitParametersWithOptionalType(ctx: ParametersWithOptionalTypeContext) {}

    override fun enterParameterWithOptionalType(ctx: ParameterWithOptionalTypeContext) {}

    override fun exitParameterWithOptionalType(ctx: ParameterWithOptionalTypeContext) {}

    override fun enterParameter(ctx: ParameterContext) {}

    override fun exitParameter(ctx: ParameterContext) {}

    override fun enterObjectDeclaration(ctx: ObjectDeclarationContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitObjectDeclaration(ctx: ObjectDeclarationContext) {}

    override fun enterSecondaryConstructor(ctx: SecondaryConstructorContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitSecondaryConstructor(ctx: SecondaryConstructorContext) {}

    override fun enterConstructorDelegationCall(ctx: ConstructorDelegationCallContext) {}

    override fun exitConstructorDelegationCall(ctx: ConstructorDelegationCallContext) {}

    override fun enterEnumClassBody(ctx: EnumClassBodyContext) {}

    override fun exitEnumClassBody(ctx: EnumClassBodyContext) {}

    override fun enterEnumEntries(ctx: EnumEntriesContext) {}

    override fun exitEnumEntries(ctx: EnumEntriesContext) {}

    override fun enterEnumEntry(ctx: EnumEntryContext) {}

    override fun exitEnumEntry(ctx: EnumEntryContext) {}

    override fun enterType(ctx: TypeContext) {
        enterIdentifierHighlighting(RecogizedElements.TypeIdentifier)
    }

    override fun exitType(ctx: TypeContext) {}

    override fun enterTypeReference(ctx: TypeReferenceContext) {
        enterIdentifierHighlighting(RecogizedElements.TypeIdentifier)
    }

    override fun exitTypeReference(ctx: TypeReferenceContext) {}

    override fun enterNullableType(ctx: NullableTypeContext) {}

    override fun exitNullableType(ctx: NullableTypeContext) {}

    override fun enterQuest(ctx: QuestContext) {}

    override fun exitQuest(ctx: QuestContext) {}

    override fun enterUserType(ctx: UserTypeContext) {
        enterIdentifierHighlighting(RecogizedElements.TypeIdentifier)
    }

    override fun exitUserType(ctx: UserTypeContext) {}

    override fun enterSimpleUserType(ctx: SimpleUserTypeContext) {
        enterIdentifierHighlighting(RecogizedElements.TypeIdentifier)
    }

    override fun exitSimpleUserType(ctx: SimpleUserTypeContext) {}

    override fun enterTypeProjection(ctx: TypeProjectionContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitTypeProjection(ctx: TypeProjectionContext) {}

    override fun enterTypeProjectionModifiers(ctx: TypeProjectionModifiersContext) {}

    override fun exitTypeProjectionModifiers(ctx: TypeProjectionModifiersContext) {}

    override fun enterTypeProjectionModifier(ctx: TypeProjectionModifierContext) {}

    override fun exitTypeProjectionModifier(ctx: TypeProjectionModifierContext) {}

    override fun enterFunctionType(ctx: FunctionTypeContext) {}

    override fun exitFunctionType(ctx: FunctionTypeContext) {}

    override fun enterFunctionTypeParameters(ctx: FunctionTypeParametersContext) {}

    override fun exitFunctionTypeParameters(ctx: FunctionTypeParametersContext) {}

    override fun enterParenthesizedType(ctx: ParenthesizedTypeContext) {}

    override fun exitParenthesizedType(ctx: ParenthesizedTypeContext) {
        addHighlightingParenthesis(ctx)
    }

    override fun enterReceiverType(ctx: ReceiverTypeContext) {}

    override fun exitReceiverType(ctx: ReceiverTypeContext) {}

    override fun enterParenthesizedUserType(ctx: ParenthesizedUserTypeContext) {}

    override fun exitParenthesizedUserType(ctx: ParenthesizedUserTypeContext) {
        addHighlightingParenthesis(ctx)
    }

    override fun enterStatements(ctx: StatementsContext) {}

    override fun exitStatements(ctx: StatementsContext) {}

    override fun enterStatement(ctx: StatementContext) {}

    override fun exitStatement(ctx: StatementContext) {}

    override fun enterLabel(ctx: LabelContext) {}

    override fun exitLabel(ctx: LabelContext) {
        addHighlightingElement(ctx, RecogizedElements.Label)
    }

    override fun enterControlStructureBody(ctx: ControlStructureBodyContext) {}

    override fun exitControlStructureBody(ctx: ControlStructureBodyContext) {}

    override fun enterBlock(ctx: BlockContext) {
        exitHighlightingElement(ctx)
    }

    override fun exitBlock(ctx: BlockContext) {}

    override fun enterLoopStatement(ctx: LoopStatementContext) {}

    override fun exitLoopStatement(ctx: LoopStatementContext) {}

    override fun enterForStatement(ctx: ForStatementContext) {
        highlightKeywordWithOffset(ctx, ctx.start.startIndex, FOR_TOKEN_LENGTH)
    }

    override fun exitForStatement(ctx: ForStatementContext) {}

    override fun enterWhileStatement(ctx: WhileStatementContext) {
        highlightKeywordWithOffset(ctx, ctx.start.startIndex, WHILE_TOKEN_LENGTH)
    }

    override fun exitWhileStatement(ctx: WhileStatementContext) {}

    override fun enterDoWhileStatement(ctx: DoWhileStatementContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitDoWhileStatement(ctx: DoWhileStatementContext) {
        ctx.WHILE()?.let {
            highlightKeywordWithOffset(ctx, it.symbol.startIndex, WHILE_TOKEN_LENGTH)
        }
    }

    override fun enterAssignment(ctx: AssignmentContext) {}

    override fun exitAssignment(ctx: AssignmentContext) {}

    override fun enterSemi(ctx: SemiContext) {}

    override fun exitSemi(ctx: SemiContext) {}

    override fun enterSemis(ctx: SemisContext) {}

    override fun exitSemis(ctx: SemisContext) {}

    override fun enterExpression(ctx: ExpressionContext) {}

    override fun exitExpression(ctx: ExpressionContext) {}

    override fun enterDisjunction(ctx: DisjunctionContext) {}

    override fun exitDisjunction(ctx: DisjunctionContext) {}

    override fun enterConjunction(ctx: ConjunctionContext) {}

    override fun exitConjunction(ctx: ConjunctionContext) {}

    override fun enterEquality(ctx: EqualityContext) {}

    override fun exitEquality(ctx: EqualityContext) {}

    override fun enterComparison(ctx: ComparisonContext) {}

    override fun exitComparison(ctx: ComparisonContext) {}

    override fun enterInfixOperation(ctx: InfixOperationContext) {}

    override fun exitInfixOperation(ctx: InfixOperationContext) {}

    override fun enterElvisExpression(ctx: ElvisExpressionContext) {}

    override fun exitElvisExpression(ctx: ElvisExpressionContext) {}

    override fun enterElvis(ctx: ElvisContext) {}

    override fun exitElvis(ctx: ElvisContext) {}

    override fun enterInfixFunctionCall(ctx: InfixFunctionCallContext) {}

    override fun exitInfixFunctionCall(ctx: InfixFunctionCallContext) {}

    override fun enterRangeExpression(ctx: RangeExpressionContext) {}

    override fun exitRangeExpression(ctx: RangeExpressionContext) {}

    override fun enterAdditiveExpression(ctx: AdditiveExpressionContext) {}

    override fun exitAdditiveExpression(ctx: AdditiveExpressionContext) {}

    override fun enterMultiplicativeExpression(ctx: MultiplicativeExpressionContext) {}

    override fun exitMultiplicativeExpression(ctx: MultiplicativeExpressionContext) {}

    override fun enterAsExpression(ctx: AsExpressionContext) {}

    override fun exitAsExpression(ctx: AsExpressionContext) {}

    override fun enterPrefixUnaryExpression(ctx: PrefixUnaryExpressionContext) {}

    override fun exitPrefixUnaryExpression(ctx: PrefixUnaryExpressionContext) {}

    override fun enterUnaryPrefix(ctx: UnaryPrefixContext) {}

    override fun exitUnaryPrefix(ctx: UnaryPrefixContext) {}

    override fun enterPostfixUnaryExpression(ctx: PostfixUnaryExpressionContext) {}

    override fun exitPostfixUnaryExpression(ctx: PostfixUnaryExpressionContext) {}

    override fun enterPostfixUnarySuffix(ctx: PostfixUnarySuffixContext) {}

    override fun exitPostfixUnarySuffix(ctx: PostfixUnarySuffixContext) {}

    override fun enterDirectlyAssignableExpression(ctx: DirectlyAssignableExpressionContext) {}

    override fun exitDirectlyAssignableExpression(ctx: DirectlyAssignableExpressionContext) {}

    override fun enterParenthesizedDirectlyAssignableExpression(ctx: ParenthesizedDirectlyAssignableExpressionContext) {}

    override fun exitParenthesizedDirectlyAssignableExpression(ctx: ParenthesizedDirectlyAssignableExpressionContext) {}

    override fun enterAssignableExpression(ctx: AssignableExpressionContext) {}

    override fun exitAssignableExpression(ctx: AssignableExpressionContext) {}

    override fun enterParenthesizedAssignableExpression(ctx: ParenthesizedAssignableExpressionContext) {}

    override fun exitParenthesizedAssignableExpression(ctx: ParenthesizedAssignableExpressionContext) {}

    override fun enterAssignableSuffix(ctx: AssignableSuffixContext) {}

    override fun exitAssignableSuffix(ctx: AssignableSuffixContext) {}

    override fun enterIndexingSuffix(ctx: IndexingSuffixContext) {}

    override fun exitIndexingSuffix(ctx: IndexingSuffixContext) {}

    override fun enterNavigationSuffix(ctx: NavigationSuffixContext) {}

    override fun exitNavigationSuffix(ctx: NavigationSuffixContext) {}

    override fun enterCallSuffix(ctx: CallSuffixContext) {
        if (lastElement == RecogizedElements.Identifier) {
            result.add(ElementWithPos(lastElementStart!!, ctx.start.startIndex - 1, RecogizedElements.FunctionIdentifier))
            lastElement = null
        }
    }

    override fun exitCallSuffix(ctx: CallSuffixContext) {}

    override fun enterAnnotatedLambda(ctx: AnnotatedLambdaContext) {}

    override fun exitAnnotatedLambda(ctx: AnnotatedLambdaContext) {}

    override fun enterTypeArguments(ctx: TypeArgumentsContext) {}

    override fun exitTypeArguments(ctx: TypeArgumentsContext) {}

    override fun enterValueArguments(ctx: ValueArgumentsContext) {}

    override fun exitValueArguments(ctx: ValueArgumentsContext) {}

    override fun enterValueArgument(ctx: ValueArgumentContext) {}

    override fun exitValueArgument(ctx: ValueArgumentContext) {}

    override fun enterPrimaryExpression(ctx: PrimaryExpressionContext) {}

    override fun exitPrimaryExpression(ctx: PrimaryExpressionContext) {}

    override fun enterParenthesizedExpression(ctx: ParenthesizedExpressionContext) {}

    override fun exitParenthesizedExpression(ctx: ParenthesizedExpressionContext) {
        addHighlightingParenthesis(ctx)
    }

    override fun enterCollectionLiteral(ctx: CollectionLiteralContext) {}

    override fun exitCollectionLiteral(ctx: CollectionLiteralContext) {}

    override fun enterLiteralConstant(ctx: LiteralConstantContext) {}

    override fun exitLiteralConstant(ctx: LiteralConstantContext) {
        addHighlightingElement(ctx, RecogizedElements.Number)
    }

    override fun enterStringLiteral(ctx: StringLiteralContext) {}

    override fun exitStringLiteral(ctx: StringLiteralContext) {
        addHighlightingElement(ctx, RecogizedElements.String)
    }

    override fun enterLineStringLiteral(ctx: LineStringLiteralContext) {}

    override fun exitLineStringLiteral(ctx: LineStringLiteralContext) {
        addHighlightingElement(ctx, RecogizedElements.String)
    }

    override fun enterMultiLineStringLiteral(ctx: MultiLineStringLiteralContext) {}

    override fun exitMultiLineStringLiteral(ctx: MultiLineStringLiteralContext) {
        addHighlightingElement(ctx, RecogizedElements.String)
    }

    override fun enterLineStringContent(ctx: LineStringContentContext) {}

    override fun exitLineStringContent(ctx: LineStringContentContext) {}

    override fun enterLineStringExpression(ctx: LineStringExpressionContext) {}

    override fun exitLineStringExpression(ctx: LineStringExpressionContext) {
        addHighlightingElement(ctx, RecogizedElements.StringTemplate)
    }

    override fun enterMultiLineStringContent(ctx: MultiLineStringContentContext) {}

    override fun exitMultiLineStringContent(ctx: MultiLineStringContentContext) {}

    override fun enterMultiLineStringExpression(ctx: MultiLineStringExpressionContext) {}

    override fun exitMultiLineStringExpression(ctx: MultiLineStringExpressionContext) {
        addHighlightingElement(ctx, RecogizedElements.StringTemplate)
    }

    override fun enterLambdaLiteral(ctx: LambdaLiteralContext) {}

    override fun exitLambdaLiteral(ctx: LambdaLiteralContext) {}

    override fun enterLambdaParameters(ctx: LambdaParametersContext) {}

    override fun exitLambdaParameters(ctx: LambdaParametersContext) {}

    override fun enterLambdaParameter(ctx: LambdaParameterContext) {}

    override fun exitLambdaParameter(ctx: LambdaParameterContext) {}

    override fun enterAnonymousFunction(ctx: AnonymousFunctionContext) {}

    override fun exitAnonymousFunction(ctx: AnonymousFunctionContext) {}

    override fun enterFunctionLiteral(ctx: FunctionLiteralContext) {}

    override fun exitFunctionLiteral(ctx: FunctionLiteralContext) {}

    override fun enterObjectLiteral(ctx: ObjectLiteralContext) {
        enterIdentifierHighlighting(RecogizedElements.TypeIdentifier)
        enterHighlightingKeyword(ctx)
    }

    override fun exitObjectLiteral(ctx: ObjectLiteralContext) {}

    override fun enterThisExpression(ctx: ThisExpressionContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitThisExpression(ctx: ThisExpressionContext) {}

    override fun enterSuperExpression(ctx: SuperExpressionContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitSuperExpression(ctx: SuperExpressionContext) {}

    override fun enterIfExpression(ctx: IfExpressionContext) {
        highlightKeywordWithOffset(ctx, ctx.start.startIndex, IF_TOKEN_LENGTH)
    }

    override fun exitIfExpression(ctx: IfExpressionContext) {
        ctx.ELSE()?.let {
            highlightKeywordWithOffset(ctx, it.symbol.startIndex, ELSE_TOKEN_LENGTH)
        }
    }

    override fun enterWhenSubject(ctx: WhenSubjectContext) {
        exitHighlightingElement(ctx)
    }

    override fun exitWhenSubject(ctx: WhenSubjectContext) {}

    override fun enterWhenExpression(ctx: WhenExpressionContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitWhenExpression(ctx: WhenExpressionContext) {}

    override fun enterWhenEntry(ctx: WhenEntryContext) {
        exitHighlightingElement(ctx)
        enterHighlightingKeyword(ctx)
    }

    override fun exitWhenEntry(ctx: WhenEntryContext) {}

    override fun enterWhenCondition(ctx: WhenConditionContext) {
        cancelHighlightingKeyword()
    }

    override fun exitWhenCondition(ctx: WhenConditionContext) {}

    override fun enterRangeTest(ctx: RangeTestContext) {}

    override fun exitRangeTest(ctx: RangeTestContext) {}

    override fun enterTypeTest(ctx: TypeTestContext) {}

    override fun exitTypeTest(ctx: TypeTestContext) {}

    override fun enterTryExpression(ctx: TryExpressionContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitTryExpression(ctx: TryExpressionContext) {}

    override fun enterCatchBlock(ctx: CatchBlockContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitCatchBlock(ctx: CatchBlockContext) {}

    override fun enterFinallyBlock(ctx: FinallyBlockContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitFinallyBlock(ctx: FinallyBlockContext) {}

    override fun enterJumpExpression(ctx: JumpExpressionContext) {
        enterHighlightingKeyword(ctx)
    }

    override fun exitJumpExpression(ctx: JumpExpressionContext) {}

    override fun enterCallableReference(ctx: CallableReferenceContext) {}

    override fun exitCallableReference(ctx: CallableReferenceContext) {}

    override fun enterAssignmentAndOperator(ctx: AssignmentAndOperatorContext) {}

    override fun exitAssignmentAndOperator(ctx: AssignmentAndOperatorContext) {}

    override fun enterEqualityOperator(ctx: EqualityOperatorContext) {}

    override fun exitEqualityOperator(ctx: EqualityOperatorContext) {}

    override fun enterComparisonOperator(ctx: ComparisonOperatorContext) {}

    override fun exitComparisonOperator(ctx: ComparisonOperatorContext) {}

    override fun enterInOperator(ctx: InOperatorContext) {}

    override fun exitInOperator(ctx: InOperatorContext) {}

    override fun enterIsOperator(ctx: IsOperatorContext) {}

    override fun exitIsOperator(ctx: IsOperatorContext) {}

    override fun enterAdditiveOperator(ctx: AdditiveOperatorContext) {}

    override fun exitAdditiveOperator(ctx: AdditiveOperatorContext) {}

    override fun enterMultiplicativeOperator(ctx: MultiplicativeOperatorContext) {}

    override fun exitMultiplicativeOperator(ctx: MultiplicativeOperatorContext) {}

    override fun enterAsOperator(ctx: AsOperatorContext) {}

    override fun exitAsOperator(ctx: AsOperatorContext) {}

    override fun enterPrefixUnaryOperator(ctx: PrefixUnaryOperatorContext) {}

    override fun exitPrefixUnaryOperator(ctx: PrefixUnaryOperatorContext) {}

    override fun enterPostfixUnaryOperator(ctx: PostfixUnaryOperatorContext) {}

    override fun exitPostfixUnaryOperator(ctx: PostfixUnaryOperatorContext) {}

    override fun enterExcl(ctx: ExclContext) {}

    override fun exitExcl(ctx: ExclContext) {}

    override fun enterMemberAccessOperator(ctx: MemberAccessOperatorContext) {}

    override fun exitMemberAccessOperator(ctx: MemberAccessOperatorContext) {}

    override fun enterSafeNav(ctx: SafeNavContext) {}

    override fun exitSafeNav(ctx: SafeNavContext) {}

    override fun enterModifiers(ctx: ModifiersContext) {}

    override fun exitModifiers(ctx: ModifiersContext) {}

    override fun enterParameterModifiers(ctx: ParameterModifiersContext) {}

    override fun exitParameterModifiers(ctx: ParameterModifiersContext) {}

    override fun enterModifier(ctx: ModifierContext) {}

    override fun exitModifier(ctx: ModifierContext) {}

    override fun enterTypeModifiers(ctx: TypeModifiersContext) {}

    override fun exitTypeModifiers(ctx: TypeModifiersContext) {}

    override fun enterTypeModifier(ctx: TypeModifierContext) {}

    override fun exitTypeModifier(ctx: TypeModifierContext) {}

    override fun enterClassModifier(ctx: ClassModifierContext) {}

    override fun exitClassModifier(ctx: ClassModifierContext) {}

    override fun enterMemberModifier(ctx: MemberModifierContext) {}

    override fun exitMemberModifier(ctx: MemberModifierContext) {}

    override fun enterVisibilityModifier(ctx: VisibilityModifierContext) {}

    override fun exitVisibilityModifier(ctx: VisibilityModifierContext) {}

    override fun enterVarianceModifier(ctx: VarianceModifierContext) {}

    override fun exitVarianceModifier(ctx: VarianceModifierContext) {}

    override fun enterTypeParameterModifiers(ctx: TypeParameterModifiersContext) {}

    override fun exitTypeParameterModifiers(ctx: TypeParameterModifiersContext) {}

    override fun enterTypeParameterModifier(ctx: TypeParameterModifierContext) {}

    override fun exitTypeParameterModifier(ctx: TypeParameterModifierContext) {}

    override fun enterFunctionModifier(ctx: FunctionModifierContext) {}

    override fun exitFunctionModifier(ctx: FunctionModifierContext) {}

    override fun enterPropertyModifier(ctx: PropertyModifierContext) {}

    override fun exitPropertyModifier(ctx: PropertyModifierContext) {}

    override fun enterInheritanceModifier(ctx: InheritanceModifierContext) {}

    override fun exitInheritanceModifier(ctx: InheritanceModifierContext) {}

    override fun enterParameterModifier(ctx: ParameterModifierContext) {}

    override fun exitParameterModifier(ctx: ParameterModifierContext) {}

    override fun enterReificationModifier(ctx: ReificationModifierContext) {}

    override fun exitReificationModifier(ctx: ReificationModifierContext) {}

    override fun enterPlatformModifier(ctx: PlatformModifierContext) {}

    override fun exitPlatformModifier(ctx: PlatformModifierContext) {}

    override fun enterAnnotation(ctx: AnnotationContext) {}

    override fun exitAnnotation(ctx: AnnotationContext) {}

    override fun enterSingleAnnotation(ctx: SingleAnnotationContext) {}

    override fun exitSingleAnnotation(ctx: SingleAnnotationContext) {}

    override fun enterMultiAnnotation(ctx: MultiAnnotationContext) {}

    override fun exitMultiAnnotation(ctx: MultiAnnotationContext) {}

    override fun enterAnnotationUseSiteTarget(ctx: AnnotationUseSiteTargetContext) {}

    override fun exitAnnotationUseSiteTarget(ctx: AnnotationUseSiteTargetContext) {}

    override fun enterUnescapedAnnotation(ctx: UnescapedAnnotationContext) {}

    override fun exitUnescapedAnnotation(ctx: UnescapedAnnotationContext) {}

    override fun enterSimpleIdentifier(ctx: SimpleIdentifierContext) {}

    override fun exitSimpleIdentifier(ctx: SimpleIdentifierContext) {
        addHighlightingIdentifier(ctx)
    }

    override fun enterIdentifier(ctx: IdentifierContext) {}

    override fun exitIdentifier(ctx: IdentifierContext) {
        addHighlightingIdentifier(ctx)
    }

    override fun enterEveryRule(ctx: ParserRuleContext) {}

    override fun exitEveryRule(ctx: ParserRuleContext) {}

    override fun visitTerminal(node: TerminalNode) {}

    override fun visitErrorNode(node: ErrorNode) {}
}