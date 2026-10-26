package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.utils.Token.Operator
import java.util.EnumMap
import kotlin.math.pow

private sealed interface Token {
    val precedence: Int
        get() = Precedence.LOWEST

    data class Operand(val value: String) : Token
    object LParen : Token
    object RParen : Token

    enum class Operator(override val precedence: Int) : Token {
        Plus(Precedence.SUM),
        Minus(Precedence.SUM),

        LogicalNot(Precedence.PREFIX),
        GreaterThan(Precedence.PREFIX),
        LessThan(Precedence.PREFIX),

        Multiply(Precedence.PRODUCT),
        Divide(Precedence.PRODUCT),
        Power(Precedence.EXPONENT),
        LogicalAnd(Precedence.AND),
        LogicalOr(Precedence.OR);
    }
}

private object Precedence {
    const val LOWEST = 0
    const val OR = 1
    const val AND = 2
    const val SUM = 3
    const val PRODUCT = 4
    const val EXPONENT = 5
    const val PREFIX = 6
}

enum class UnaryOperators {
    Plus,
    Minus,

    LogicalNot,
    GreaterThan,
    LessThan;
}

enum class BinaryOperators {
    Plus,
    Minus,
    Multiply,
    Divide,
    Power,

    LogicalAnd,
    LogicalOr;
}

enum class EvaluationResultType {
    Numeric,
    Computed,
    Either {
        override fun accepts(other: EvaluationResultType): Boolean {
            return true
        }

        override fun fulfills(other: EvaluationResultType): Boolean {
            return true
        }
    };

    open fun accepts(other: EvaluationResultType) = this == other
    open fun fulfills(other: EvaluationResultType) = this == other || other == Either
}

sealed interface EvaluationResult<out T> {
    fun getNumeric(): Double
    fun getComputed(): T
    fun getType(): EvaluationResultType

    data class NumericResult(val value: Double) : EvaluationResult<Nothing> {
        override fun getNumeric(): Double = value
        override fun getComputed() = throw ClassCastException()
        override fun getType() = EvaluationResultType.Numeric
    }

    data class ComputedResult<T>(val value: T) : EvaluationResult<T> {
        override fun getNumeric(): Double = throw ClassCastException()
        override fun getComputed(): T = value
        override fun getType() = EvaluationResultType.Computed
    }
}

class ExpressionEvaluator<T> private constructor(
    private val operand: ((String) -> T)? = null,
    private val unary: Map<UnaryOperators, (EvaluationResult<T>) -> EvaluationResult<T>>? = null,
    private val binary: Map<BinaryOperators, (EvaluationResult<T>, EvaluationResult<T>) -> EvaluationResult<T>>? = null,
    private val parent: ExpressionEvaluator<T>? = null,
    @Suppress("unused")
    unused: Boolean,
) {
    constructor(
        operand: ((String) -> T)? = null,
        unary: Map<UnaryOperators, (EvaluationResult<T>) -> EvaluationResult<T>>? = null,
        binary: Map<BinaryOperators, (EvaluationResult<T>, EvaluationResult<T>) -> EvaluationResult<T>>? = null,
        parent: ExpressionEvaluator<T>? = null,
    ) : this(
        operand,
        unary?.let { EnumMap(it) },
        binary?.let { EnumMap(it) },
        parent,
        true,
    )

    fun getOperandEvaluator(): ((String) -> T)? {
        return operand ?: parent?.getOperandEvaluator()
    }

    fun getUnaryEvaluator(op: UnaryOperators): ((EvaluationResult<T>) -> EvaluationResult<T>)? {
        return unary?.get(op) ?: parent?.getUnaryEvaluator(op)
    }

    fun getBinaryEvaluator(op: BinaryOperators): ((EvaluationResult<T>, EvaluationResult<T>) -> EvaluationResult<T>)? {
        return binary?.get(op) ?: parent?.getBinaryEvaluator(op)
    }

    companion object {
        val Numeric = ExpressionEvaluator<Nothing>(
            null,
            mapOf(
                UnaryOperators.Plus to { it as EvaluationResult.NumericResult },
                UnaryOperators.Minus to { EvaluationResult.NumericResult(-it.getNumeric()) },
            ),
            mapOf(
                BinaryOperators.Plus to { a, b -> EvaluationResult.NumericResult(a.getNumeric() + b.getNumeric()) },
                BinaryOperators.Minus to { a, b -> EvaluationResult.NumericResult(a.getNumeric() - b.getNumeric()) },
                BinaryOperators.Multiply to { a, b -> EvaluationResult.NumericResult(a.getNumeric() * b.getNumeric()) },
                BinaryOperators.Divide to { a, b -> EvaluationResult.NumericResult(a.getNumeric() / b.getNumeric()) },
                BinaryOperators.Power to { a, b -> EvaluationResult.NumericResult(a.getNumeric().pow(b.getNumeric())) },
            ),
        )

        fun <T> getNumeric1(): ExpressionEvaluator<T> {
            @Suppress("UNCHECKED_CAST")
            return Numeric as ExpressionEvaluator<T>
        }

        fun <T> wrap(
            operand: ((String) -> T)? = null,
            unary: Map<UnaryOperators, (T) -> T>? = null,
            binary: Map<BinaryOperators, (T, T) -> T>? = null,
            parent: ExpressionEvaluator<T>? = null,
        ): ExpressionEvaluator<T> {
            return ExpressionEvaluator(
                operand?.let { f -> { f(it) } },
                unary?.entries?.associate { (k, v) -> k to {
                    EvaluationResult.ComputedResult(v(it.getComputed()))
                } },
                binary?.entries?.associate { (k, v) -> k to { a, b ->
                    EvaluationResult.ComputedResult(v(a.getComputed(), b.getComputed()))
                } },
                parent,
            )
        }
    }
}

class ExpressionValidator private constructor(
    private val operand: EvaluationResultType? = null,
    private val unary: Map<UnaryOperators, Pair<EvaluationResultType, EvaluationResultType>>? = null,
    private val binary: Map<BinaryOperators, Pair<EvaluationResultType, EvaluationResultType>>? = null,
    private val parent: ExpressionValidator? = null,
    @Suppress("unused")
    unused: Boolean,
) {
    constructor(
        operand: EvaluationResultType? = null,
        unary: Map<UnaryOperators, Pair<EvaluationResultType, EvaluationResultType>>? = null,
        binary: Map<BinaryOperators, Pair<EvaluationResultType, EvaluationResultType>>? = null,
        parent: ExpressionValidator? = null,
    ) : this(
        operand,
        unary?.let { EnumMap(it) },
        binary?.let { EnumMap(it) },
        parent,
        true,
    )

    fun getOperandType(): EvaluationResultType? {
        return operand ?: parent?.getOperandType()
    }

    fun getUnaryType(op: UnaryOperators): Pair<EvaluationResultType, EvaluationResultType>? {
        return unary?.get(op) ?: parent?.getUnaryType(op)
    }

    fun getBinaryType(op: BinaryOperators): Pair<EvaluationResultType, EvaluationResultType>? {
        return binary?.get(op) ?: parent?.getBinaryType(op)
    }

    companion object {
        val Numeric = ExpressionValidator(
            EvaluationResultType.Numeric,
            mapOf(
                UnaryOperators.Plus to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
                UnaryOperators.Minus to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
            ),
            mapOf(
                BinaryOperators.Plus to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
                BinaryOperators.Minus to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
                BinaryOperators.Multiply to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
                BinaryOperators.Divide to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
                BinaryOperators.Power to (EvaluationResultType.Numeric to EvaluationResultType.Numeric),
            ),
        )
        val Computed = ExpressionValidator(
            EvaluationResultType.Computed,
            UnaryOperators.entries.associateWith { (EvaluationResultType.Computed to EvaluationResultType.Computed) },
            BinaryOperators.entries.associateWith { (EvaluationResultType.Computed to EvaluationResultType.Computed) },
        )
    }
}

private sealed interface Expr {
    fun <T> evaluate(
        evaluator: ExpressionEvaluator<T>,
        validator: ExpressionValidator,
        expected: EvaluationResultType,
    ): EvaluationResult<T>

    fun validate(validator: ExpressionValidator, expected: EvaluationResultType): Boolean
}

private data class OperandNode(val value: String) : Expr {
    override fun <T> evaluate(
        evaluator: ExpressionEvaluator<T>,
        validator: ExpressionValidator,
        expected: EvaluationResultType
    ): EvaluationResult<T> {
        if (expected == EvaluationResultType.Numeric)
            return EvaluationResult.NumericResult(StringUtils.tryParseShortenedNumber(value) ?: Double.NaN)
        val result = EvaluationResult.ComputedResult(evaluator.getOperandEvaluator()!!(value))
        if (!expected.accepts(result.getType())) throw IllegalStateException()
        if (!validator.getOperandType()!!.accepts(result.getType())) throw IllegalStateException()
        return result
    }

    override fun validate(
        validator: ExpressionValidator,
        expected: EvaluationResultType
    ): Boolean {
        return validator.getOperandType()!!.fulfills(expected)
    }
}

private data class UnaryOpNode(val operator: Operator, val operand: Expr) : Expr {
    private val operatorType = when (operator) {
        Operator.Plus -> UnaryOperators.Plus
        Operator.Minus -> UnaryOperators.Minus
        Operator.LogicalNot -> UnaryOperators.LogicalNot
        Operator.GreaterThan -> UnaryOperators.GreaterThan
        Operator.LessThan -> UnaryOperators.LessThan
        else -> throw IllegalStateException()
    }

    override fun <T> evaluate(
        evaluator: ExpressionEvaluator<T>,
        validator: ExpressionValidator,
        expected: EvaluationResultType
    ): EvaluationResult<T> {
        val type = validator.getUnaryType(operatorType)!!
        val arg = operand.evaluate(evaluator, validator, type.first)
        val result = evaluator.getUnaryEvaluator(operatorType)!!.invoke(arg)
        if (!expected.accepts(result.getType())) throw IllegalStateException()
        if (!type.second.accepts(result.getType())) throw IllegalStateException()
        return result
    }

    override fun validate(
        validator: ExpressionValidator,
        expected: EvaluationResultType
    ): Boolean {
        val type = validator.getUnaryType(operatorType)!!
        return type.second.fulfills(expected) && operand.validate(validator, type.first)
    }
}

private data class BinaryOpNode(val left: Expr, val operator: Operator, val right: Expr) : Expr {
    private val operatorType = when (operator) {
        Operator.Plus -> BinaryOperators.Plus
        Operator.Minus -> BinaryOperators.Minus
        Operator.Multiply -> BinaryOperators.Multiply
        Operator.Divide -> BinaryOperators.Divide
        Operator.Power -> BinaryOperators.Power
        Operator.LogicalAnd -> BinaryOperators.LogicalAnd
        Operator.LogicalOr -> BinaryOperators.LogicalOr
        else -> throw IllegalStateException()
    }

    override fun <T> evaluate(
        evaluator: ExpressionEvaluator<T>,
        validator: ExpressionValidator,
        expected: EvaluationResultType
    ): EvaluationResult<T> {
        val type = validator.getBinaryType(operatorType)!!
        val argL = left.evaluate(evaluator, validator, type.first)
        val argR = right.evaluate(evaluator, validator, type.first)
        val result = evaluator.getBinaryEvaluator(operatorType)!!.invoke(argL, argR)
        if (!expected.accepts(result.getType())) throw IllegalStateException()
        if (!type.second.accepts(result.getType())) throw IllegalStateException()
        return result
    }

    override fun validate(
        validator: ExpressionValidator,
        expected: EvaluationResultType
    ): Boolean {
        val type = validator.getBinaryType(operatorType)!!
        return type.second.fulfills(expected) &&
            left.validate(validator, type.first) &&
            right.validate(validator, type.first)
    }
}

class ExprTree private constructor(tokens: PeekingIterator<Token>) {
    private val root: Expr

    fun <T> evaluate(evaluator: ExpressionEvaluator<T>, validator: ExpressionValidator): EvaluationResult<T>? {
        return runCatching { root.evaluate(evaluator, validator, EvaluationResultType.Either) }.getOrNull()
    }

    fun validate(validator: ExpressionValidator): Boolean {
        return root.validate(validator, EvaluationResultType.Either)
    }

    init {
        fun parseExpression(precedence: Int): Expr {
            var left = when (val token = tokens.next()) {
                is Token.Operand -> OperandNode(token.value)
                is Token.LParen -> parseExpression(Precedence.LOWEST).also {
                    require(tokens.next() is Token.RParen)
                }

                Operator.Plus,
                Operator.Minus,
                Operator.GreaterThan,
                Operator.LessThan,
                    -> UnaryOpNode(token as Operator, parseExpression(Precedence.PREFIX))

                else -> throw IllegalArgumentException()
            }

            while (tokens.hasNext() && precedence < tokens.peek().precedence) {
                val opToken = tokens.next()
                left = when (opToken) {
                    Operator.Plus,
                    Operator.Minus,
                    Operator.Multiply,
                    Operator.Divide,
                    Operator.LogicalAnd,
                    Operator.LogicalOr,
                        -> BinaryOpNode(left, opToken as Operator, parseExpression(opToken.precedence))

                    Operator.Power ->
                        BinaryOpNode(left, opToken as Operator, parseExpression(opToken.precedence - 1))

                    else -> throw IllegalArgumentException()
                }
            }

            return left
        }

        root = parseExpression(Precedence.LOWEST)
        if (tokens.hasNext()) throw IllegalArgumentException()
    }

    private class StringIterator(input: String) {
        private val arr = input.toCharArray()
        private var idx = 0

        fun hasNext() = idx < arr.size
        fun next() = arr[idx++]
        fun peek() = arr[idx]

        fun hasAfter() = idx + 1 < arr.size
        fun peekAfter() = arr[idx + 1]
    }

    private interface PeekingIterator<T> : Iterator<T> {
        fun peek(): T
    }

    companion object {
        fun parse(input: String): ExprTree? {
            return runCatching {
                ExprTree(
                    tokenize(input).let {
                        object : PeekingIterator<Token> {
                            private var hasPeeked = false
                            private var peeked: Token? = null

                            override fun hasNext(): Boolean = hasPeeked || it.hasNext()

                            override fun next(): Token  {
                                return if (hasPeeked) {
                                    hasPeeked = false
                                    peeked!!
                                } else {
                                    it.next()
                                }
                            }

                            override fun peek(): Token {
                                if (!hasPeeked) {
                                    peeked = it.next()
                                    hasPeeked = true
                                }
                                return peeked!!
                            }
                        }

                    }
                )
            }.getOrNull()
        }

        private fun tryConsumeToken(iter: StringIterator): Token? {
            val token = when (iter.peek()) {
                '(' -> Token.LParen
                ')' -> Token.RParen
                '+' -> Operator.Plus
                '-' -> Operator.Minus
                '*' -> Operator.Multiply
                '/' -> Operator.Divide
                '^' -> Operator.Power
                '>' -> Operator.GreaterThan
                '<' -> Operator.LessThan
                '&' -> {
                    if (iter.hasAfter() && iter.peekAfter() == '&') iter.next()
                    Operator.LogicalAnd
                }
                '|' -> {
                    if (iter.hasAfter() && iter.peekAfter() == '|') iter.next()
                    Operator.LogicalOr
                }
                else -> return null
            }
            iter.next()
            return token
        }

        private fun tokenize(input: String) = iterator {
            val iter = StringIterator(input)

            var escaped = false
            var quoted = false
            val literal = StringBuilder()
            while (iter.hasNext()) {
                if (!escaped && !quoted) tryConsumeToken(iter)?.let {
                    if (literal.isNotEmpty()) {
                        yield(Token.Operand(literal.toString()))
                        literal.clear()
                    }
                    yield(it)
                    continue
                }

                val c = iter.next()
                when {
                    escaped -> {
                        literal.append(c)
                        escaped = false
                    }

                    quoted -> {
                        if (c == '"') quoted = false
                        else literal.append(c)
                    }

                    c.isWhitespace() -> {
                        if (literal.isNotEmpty()) literal.append(c)
                    }

                    c == '\\' -> escaped = true
                    c == '"' -> quoted = true

                    else -> literal.append(c)
                }
            }

            if (literal.isNotEmpty()) yield(Token.Operand(literal.toString()))
        }
    }
}