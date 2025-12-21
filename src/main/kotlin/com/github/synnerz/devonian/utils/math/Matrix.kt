package com.github.synnerz.devonian.utils.math

// this implementation is shit please do not copy
class Matrix(val arr: Array<DoubleArray>, val rows: Int, val cols: Int) {
    constructor(rows: Int, cols: Int) : this(Array(rows) { DoubleArray(cols) }, rows, cols)
    constructor(arr: Array<DoubleArray>) : this(arr, arr.size, arr[0].size)

    fun transpose(): Matrix {
        val dst = Matrix(cols, rows)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                dst.arr[c][r] = arr[r][c]
            }
        }
        return dst
    }

    fun mult(B: Matrix): Matrix {
        if (cols != B.rows) throw IllegalArgumentException("cant multiply")

        val dst = Matrix(rows, B.cols)
        for (i in 0 until rows) {
            for (j in 0 until B.cols) {
                for (k in 0 until cols) {
                    dst.arr[i][j] += arr[i][k] * B.arr[k][j]
                }
            }
        }
        return dst
    }

    fun invert(): Matrix {
        if (rows != cols) throw IllegalArgumentException("not square")

        val n = rows
        val augmented = Array(n) { i ->
            DoubleArray(2 * n).apply {
                for (j in 0 until n) this[j] = arr[i][j]
                this[i + n] = 1.0
            }
        }

        for (i in 0 until n) {
            val pivot = augmented[i][i]
            if (pivot == 0.0) throw IllegalArgumentException("not invertible")

            for (j in 0 until 2 * n) {
                augmented[i][j] /= pivot
            }

            for (k in 0 until n) {
                if (k != i) {
                    val factor = augmented[k][i]
                    for (j in 0 until 2 * n) {
                        augmented[k][j] -= factor * augmented[i][j]
                    }
                }
            }
        }

        val inverse = Array(n) { i ->
            DoubleArray(n) { j ->
                augmented[i][j + n]
            }
        }

        return Matrix(inverse)

    }
}