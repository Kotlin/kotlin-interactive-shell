// Generated from KotlinParser.g4 by ANTLR 4.8
package com.github.khud.sparklin.kshell.parser

import com.github.khud.sparklin.kshell.parser.KotlinParser.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * This class provides an empty implementation of [KotlinParserListener],
 * which can be extended to create a listener which only needs to handle a subset
 * of the available methods.
 */
class KotlinParserListenerForHighlighting : KotlinParserListener {

override fun enterKotlinFile(ctx: KotlinFileContext) {}

override fun exitKotlinFile(ctx: KotlinFileContext) {}

override fun enterScript(ctx: KotlinParser.ScriptContext) {}

override fun exitScript(ctx: KotlinParser.ScriptContext) {}

override fun enterShebangLine(ctx: ShebangLineContext) {}

override fun exitShebangLine(ctx: ShebangLineContext) {}

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

override fun enterClassDeclaration(ctx: KotlinParser.ClassDeclarationContext) {}

override fun exitClassDeclaration(ctx: KotlinParser.ClassDeclarationContext) {}

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

override fun enterTypeParameter(ctx: TypeParameterContext) {}

override fun exitTypeParameter(ctx: TypeParameterContext) {}

override fun enterTypeConstraints(ctx: TypeConstraintsContext) {}

override fun exitTypeConstraints(ctx: TypeConstraintsContext) {}

override fun enterTypeConstraint(ctx: TypeConstraintContext) {}

override fun exitTypeConstraint(ctx: TypeConstraintContext) {}

override fun enterClassMemberDeclarations(ctx: ClassMemberDeclarationsContext) {}

override fun exitClassMemberDeclarations(ctx: ClassMemberDeclarationsContext) {}

override fun enterClassMemberDeclaration(ctx: ClassMemberDeclarationContext) {}

override fun exitClassMemberDeclaration(ctx: ClassMemberDeclarationContext) {}

override fun enterAnonymousInitializer(ctx: AnonymousInitializerContext) {}

override fun exitAnonymousInitializer(ctx: AnonymousInitializerContext) {}

override fun enterCompanionObject(ctx: CompanionObjectContext) {}

override fun exitCompanionObject(ctx: CompanionObjectContext) {}

override fun enterFunctionValueParameters(ctx: FunctionValueParametersContext) {}

override fun exitFunctionValueParameters(ctx: FunctionValueParametersContext) {}

override fun enterFunctionValueParameter(ctx: FunctionValueParameterContext) {}

override fun exitFunctionValueParameter(ctx: FunctionValueParameterContext) {}

override fun enterFunctionDeclaration(ctx: FunctionDeclarationContext) {}

override fun exitFunctionDeclaration(ctx: FunctionDeclarationContext) {}

override fun enterFunctionBody(ctx: FunctionBodyContext) {}

override fun exitFunctionBody(ctx: FunctionBodyContext) {}

override fun enterVariableDeclaration(ctx: VariableDeclarationContext) {}

override fun exitVariableDeclaration(ctx: VariableDeclarationContext) {}

override fun enterMultiVariableDeclaration(ctx: MultiVariableDeclarationContext) {}

override fun exitMultiVariableDeclaration(ctx: MultiVariableDeclarationContext) {}

override fun enterPropertyDeclaration(ctx: PropertyDeclarationContext) {}

override fun exitPropertyDeclaration(ctx: PropertyDeclarationContext) {}

override fun enterPropertyDelegate(ctx: PropertyDelegateContext) {}

override fun exitPropertyDelegate(ctx: PropertyDelegateContext) {}

override fun enterGetter(ctx: GetterContext) {}

override fun exitGetter(ctx: GetterContext) {}

override fun enterSetter(ctx: SetterContext) {}

override fun exitSetter(ctx: SetterContext) {}

override fun enterParametersWithOptionalType(ctx: ParametersWithOptionalTypeContext) {}

override fun exitParametersWithOptionalType(ctx: ParametersWithOptionalTypeContext) {}

override fun enterParameterWithOptionalType(ctx: ParameterWithOptionalTypeContext) {}

override fun exitParameterWithOptionalType(ctx: ParameterWithOptionalTypeContext) {}

override fun enterParameter(ctx: ParameterContext) {}

override fun exitParameter(ctx: ParameterContext) {}

override fun enterObjectDeclaration(ctx: ObjectDeclarationContext) {}

override fun exitObjectDeclaration(ctx: ObjectDeclarationContext) {}

override fun enterSecondaryConstructor(ctx: SecondaryConstructorContext) {}

override fun exitSecondaryConstructor(ctx: SecondaryConstructorContext) {}

override fun enterConstructorDelegationCall(ctx: ConstructorDelegationCallContext) {}

override fun exitConstructorDelegationCall(ctx: ConstructorDelegationCallContext) {}

override fun enterEnumClassBody(ctx: EnumClassBodyContext) {}

override fun exitEnumClassBody(ctx: EnumClassBodyContext) {}

override fun enterEnumEntries(ctx: EnumEntriesContext) {}

override fun exitEnumEntries(ctx: EnumEntriesContext) {}

override fun enterEnumEntry(ctx: EnumEntryContext) {}

override fun exitEnumEntry(ctx: EnumEntryContext) {}

override fun enterType(ctx: KotlinParser.TypeContext) {}

override fun exitType(ctx: KotlinParser.TypeContext) {}

override fun enterTypeReference(ctx: TypeReferenceContext) {}

override fun exitTypeReference(ctx: TypeReferenceContext) {}

override fun enterNullableType(ctx: NullableTypeContext) {}

override fun exitNullableType(ctx: NullableTypeContext) {}

override fun enterQuest(ctx: QuestContext) {}

override fun exitQuest(ctx: QuestContext) {}

override fun enterUserType(ctx: UserTypeContext) {}

override fun exitUserType(ctx: UserTypeContext) {}

override fun enterSimpleUserType(ctx: SimpleUserTypeContext) {}

override fun exitSimpleUserType(ctx: SimpleUserTypeContext) {}

override fun enterTypeProjection(ctx: TypeProjectionContext) {}

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

override fun exitParenthesizedType(ctx: ParenthesizedTypeContext) {}

override fun enterReceiverType(ctx: ReceiverTypeContext) {}

override fun exitReceiverType(ctx: ReceiverTypeContext) {}

override fun enterParenthesizedUserType(ctx: ParenthesizedUserTypeContext) {}

override fun exitParenthesizedUserType(ctx: ParenthesizedUserTypeContext) {}

override fun enterStatements(ctx: StatementsContext) {}

override fun exitStatements(ctx: StatementsContext) {}

override fun enterStatement(ctx: KotlinParser.StatementContext) {}

override fun exitStatement(ctx: KotlinParser.StatementContext) {}

override fun enterLabel(ctx: LabelContext) {}

override fun exitLabel(ctx: LabelContext) {}

override fun enterControlStructureBody(ctx: ControlStructureBodyContext) {}

override fun exitControlStructureBody(ctx: ControlStructureBodyContext) {}

override fun enterBlock(ctx: KotlinParser.BlockContext) {}

override fun exitBlock(ctx: KotlinParser.BlockContext) {}

override fun enterLoopStatement(ctx: LoopStatementContext) {}

override fun exitLoopStatement(ctx: LoopStatementContext) {}

override fun enterForStatement(ctx: ForStatementContext) {}

override fun exitForStatement(ctx: ForStatementContext) {}

override fun enterWhileStatement(ctx: WhileStatementContext) {}

override fun exitWhileStatement(ctx: WhileStatementContext) {}

override fun enterDoWhileStatement(ctx: DoWhileStatementContext) {}

override fun exitDoWhileStatement(ctx: DoWhileStatementContext) {}

override fun enterAssignment(ctx: AssignmentContext) {}

override fun exitAssignment(ctx: AssignmentContext) {}

override fun enterSemi(ctx: SemiContext) {}

override fun exitSemi(ctx: SemiContext) {}

override fun enterSemis(ctx: SemisContext) {}

override fun exitSemis(ctx: SemisContext) {}

override fun enterExpression(ctx: KotlinParser.ExpressionContext) {}

override fun exitExpression(ctx: KotlinParser.ExpressionContext) {}

override fun enterDisjunction(ctx: DisjunctionContext) {}

override fun exitDisjunction(ctx: DisjunctionContext) {}

override fun enterConjunction(ctx: ConjunctionContext) {}

override fun exitConjunction(ctx: ConjunctionContext) {}

override fun enterEquality(ctx: EqualityContext) {}

override fun exitEquality(ctx: EqualityContext) {}

override fun enterComparison(ctx: KotlinParser.ComparisonContext) {}

override fun exitComparison(ctx: KotlinParser.ComparisonContext) {}

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

override fun enterCallSuffix(ctx: CallSuffixContext) {}

override fun exitCallSuffix(ctx: CallSuffixContext) {}

override fun enterAnnotatedLambda(ctx: AnnotatedLambdaContext) {}

override fun exitAnnotatedLambda(ctx: AnnotatedLambdaContext) {}

override fun enterTypeArguments(ctx: TypeArgumentsContext) {}

override fun exitTypeArguments(ctx: TypeArgumentsContext) {}

override fun enterValueArguments(ctx: ValueArgumentsContext) {}

override fun exitValueArguments(ctx: ValueArgumentsContext) {}

override fun enterValueArgument(ctx: ValueArgumentContext) {}

override fun exitValueArgument(ctx: ValueArgumentContext) {}

override fun enterPrimaryExpression(ctx: KotlinParser.PrimaryExpressionContext) {}

override fun exitPrimaryExpression(ctx: KotlinParser.PrimaryExpressionContext) {}

override fun enterParenthesizedExpression(ctx: KotlinParser.ParenthesizedExpressionContext) {}

override fun exitParenthesizedExpression(ctx: KotlinParser.ParenthesizedExpressionContext) {}

override fun enterCollectionLiteral(ctx: CollectionLiteralContext) {}

override fun exitCollectionLiteral(ctx: CollectionLiteralContext) {}

override fun enterLiteralConstant(ctx: LiteralConstantContext) {}

override fun exitLiteralConstant(ctx: LiteralConstantContext) {}

override fun enterStringLiteral(ctx: KotlinParser.StringLiteralContext) {}

override fun exitStringLiteral(ctx: KotlinParser.StringLiteralContext) {}

override fun enterLineStringLiteral(ctx: LineStringLiteralContext) {}

override fun exitLineStringLiteral(ctx: LineStringLiteralContext) {}

override fun enterMultiLineStringLiteral(ctx: MultiLineStringLiteralContext) {}

override fun exitMultiLineStringLiteral(ctx: MultiLineStringLiteralContext) {}

override fun enterLineStringContent(ctx: LineStringContentContext) {}

override fun exitLineStringContent(ctx: LineStringContentContext) {}

override fun enterLineStringExpression(ctx: LineStringExpressionContext) {}

override fun exitLineStringExpression(ctx: LineStringExpressionContext) {}

override fun enterMultiLineStringContent(ctx: MultiLineStringContentContext) {}

override fun exitMultiLineStringContent(ctx: MultiLineStringContentContext) {}

override fun enterMultiLineStringExpression(ctx: MultiLineStringExpressionContext) {}

override fun exitMultiLineStringExpression(ctx: MultiLineStringExpressionContext) {}

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

override fun enterObjectLiteral(ctx: ObjectLiteralContext) {}

override fun exitObjectLiteral(ctx: ObjectLiteralContext) {}

override fun enterThisExpression(ctx: ThisExpressionContext) {}

override fun exitThisExpression(ctx: ThisExpressionContext) {}

override fun enterSuperExpression(ctx: SuperExpressionContext) {}

override fun exitSuperExpression(ctx: SuperExpressionContext) {}

override fun enterIfExpression(ctx: IfExpressionContext) {}

override fun exitIfExpression(ctx: IfExpressionContext) {}

override fun enterWhenSubject(ctx: WhenSubjectContext) {}

override fun exitWhenSubject(ctx: WhenSubjectContext) {}

override fun enterWhenExpression(ctx: WhenExpressionContext) {}

override fun exitWhenExpression(ctx: WhenExpressionContext) {}

override fun enterWhenEntry(ctx: WhenEntryContext) {}

override fun exitWhenEntry(ctx: WhenEntryContext) {}

override fun enterWhenCondition(ctx: WhenConditionContext) {}

override fun exitWhenCondition(ctx: WhenConditionContext) {}

override fun enterRangeTest(ctx: RangeTestContext) {}

override fun exitRangeTest(ctx: RangeTestContext) {}

override fun enterTypeTest(ctx: TypeTestContext) {}

override fun exitTypeTest(ctx: TypeTestContext) {}

override fun enterTryExpression(ctx: TryExpressionContext) {}

override fun exitTryExpression(ctx: TryExpressionContext) {}

override fun enterCatchBlock(ctx: CatchBlockContext) {}

override fun exitCatchBlock(ctx: CatchBlockContext) {}

override fun enterFinallyBlock(ctx: FinallyBlockContext) {}

override fun exitFinallyBlock(ctx: FinallyBlockContext) {}

override fun enterJumpExpression(ctx: JumpExpressionContext) {}

override fun exitJumpExpression(ctx: JumpExpressionContext) {}

override fun enterCallableReference(ctx: CallableReferenceContext) {}

override fun exitCallableReference(ctx: CallableReferenceContext) {}

override fun enterAssignmentAndOperator(ctx: AssignmentAndOperatorContext) {}

override fun exitAssignmentAndOperator(ctx: AssignmentAndOperatorContext) {}

override fun enterEqualityOperator(ctx: EqualityOperatorContext) {}

override fun exitEqualityOperator(ctx: EqualityOperatorContext) {}

override fun enterComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext) {}

override fun exitComparisonOperator(ctx: KotlinParser.ComparisonOperatorContext) {}

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

override fun enterAnnotation(ctx: KotlinParser.AnnotationContext) {}

override fun exitAnnotation(ctx: KotlinParser.AnnotationContext) {}

override fun enterSingleAnnotation(ctx: SingleAnnotationContext) {}

override fun exitSingleAnnotation(ctx: SingleAnnotationContext) {}

override fun enterMultiAnnotation(ctx: MultiAnnotationContext) {}

override fun exitMultiAnnotation(ctx: MultiAnnotationContext) {}

override fun enterAnnotationUseSiteTarget(ctx: AnnotationUseSiteTargetContext) {}

override fun exitAnnotationUseSiteTarget(ctx: AnnotationUseSiteTargetContext) {}

override fun enterUnescapedAnnotation(ctx: UnescapedAnnotationContext) {}

override fun exitUnescapedAnnotation(ctx: UnescapedAnnotationContext) {}

override fun enterSimpleIdentifier(ctx: SimpleIdentifierContext) {}

override fun exitSimpleIdentifier(ctx: SimpleIdentifierContext) {}

override fun enterIdentifier(ctx: KotlinParser.IdentifierContext) {}

override fun exitIdentifier(ctx: KotlinParser.IdentifierContext) {}

override fun enterEveryRule(ctx: ParserRuleContext) {}

override fun exitEveryRule(ctx: ParserRuleContext) {}

override fun visitTerminal(node: TerminalNode) {}

override fun visitErrorNode(node: ErrorNode) {}
}