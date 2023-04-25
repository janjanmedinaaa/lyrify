package com.medina.juanantonio.lyrify.common.utils

import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Build
import android.view.animation.Interpolator
import androidx.annotation.FloatRange

/**
 * This class allows the path to end at points farther than 1
 * while the stock PathInterpolator doesn't. Therefore, you can
 * create more sophisticated layout interpolators using this class
 * Originally developed for SystemUI stack view
 *
 *
 * Since path approximate is only public on Oreo+, we use a backport
 * courtesy of alexjlockwood
 */
class FreePathInterpolator(path: Path) : Interpolator {
    private var mX: FloatArray = FloatArray(1)
    private var mY: FloatArray = FloatArray(1)

    /**
     * Returns the arclength of the path we are interpolating.
     */
    var arcLength = 0f
        private set

    /**
     * Create an interpolator for an arbitrary `Path`.
     *
     * @param path The `Path` to use to make the line representing the interpolator.
     */
    init {
        initPath(path)
    }

    private fun approximate(p: Path, PRECISION: Float): FloatArray {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            p.approximate(PRECISION)
        } else PathCompat.approximate(p, PRECISION)
    }

    private fun initPath(path: Path) {
        val pointComponents = approximate(path, PRECISION)
        val numPoints = pointComponents.size / 3
        mX = FloatArray(numPoints)
        mY = FloatArray(numPoints)
        arcLength = 0f
        var prevX = 0f
        var prevY = 0f
        var prevFraction = 0f
        var componentIndex = 0
        for (i in 0 until numPoints) {
            val fraction = pointComponents[componentIndex++]
            val x = pointComponents[componentIndex++]
            val y = pointComponents[componentIndex++]
            require(!(fraction == prevFraction && x != prevX)) { "The Path cannot have discontinuity in the X axis." }
            require(x >= prevX) { "The Path cannot loop back on itself." }
            mX[i] = x
            mY[i] = y
            arcLength += Math.hypot((x - prevX).toDouble(), (y - prevY).toDouble()).toFloat()
            prevX = x
            prevY = y
            prevFraction = fraction
        }
    }

    /**
     * Using the line in the Path in this interpolator that can be described as
     * `y = f(x)`, finds the y coordinate of the line given `t`
     * as the x coordinate.
     *
     * @param t Treated as the x coordinate along the line.
     * @return The y coordinate of the Path along the line where x = `t`.
     */
    override fun getInterpolation(t: Float): Float {
        var startIndex = 0
        var endIndex = mX.size - 1

        // Return early if out of bounds
        if (t <= 0) {
            return mY[startIndex]
        } else if (t >= 1) {
            return mY[endIndex]
        }

        // Do a binary search for the correct x to interpolate between.
        while (endIndex - startIndex > 1) {
            val midIndex = (startIndex + endIndex) / 2
            if (t < mX[midIndex]) {
                endIndex = midIndex
            } else {
                startIndex = midIndex
            }
        }
        val xRange = mX[endIndex] - mX[startIndex]
        if (xRange == 0f) {
            return mY[startIndex]
        }
        val tInRange = t - mX[startIndex]
        val fraction = tInRange / xRange
        val startY = mY[startIndex]
        val endY = mY[endIndex]
        return startY + fraction * (endY - startY)
    }

    /**
     * Finds the x that provides the given `y = f(x)`.
     *
     * @param y a value from (0,1) that is in this path.
     */
    fun getX(y: Float): Float {
        var startIndex = 0
        var endIndex = mY.size - 1

        // Return early if out of bounds
        if (y <= 0) {
            return mX[endIndex]
        } else if (y >= 1) {
            return mX[startIndex]
        }

        // Do a binary search for index that bounds the y
        while (endIndex - startIndex > 1) {
            val midIndex = (startIndex + endIndex) / 2
            if (y < mY[midIndex]) {
                startIndex = midIndex
            } else {
                endIndex = midIndex
            }
        }
        val yRange = mY[endIndex] - mY[startIndex]
        if (yRange == 0f) {
            return mX[startIndex]
        }
        val tInRange = y - mY[startIndex]
        val fraction = tInRange / yRange
        val startX = mX[startIndex]
        val endX = mX[endIndex]
        return startX + fraction * (endX - startX)
    }

    //https://gist.github.com/alexjlockwood/7d3685fe9ce7dcfde33112c4e6c5ce4f
    private object PathCompat {
        private const val MAX_NUM_POINTS = 100
        private const val FRACTION_OFFSET = 0
        private const val X_OFFSET = 1
        private const val Y_OFFSET = 2
        private const val NUM_COMPONENTS = 3

        /**
         * Approximate the `Path` with a series of line segments.
         * This returns float[] with the array containing point components.
         * There are three components for each point, in order:
         *
         *  * Fraction along the length of the path that the point resides
         *  * The x coordinate of the point
         *  * The y coordinate of the point
         *
         *
         * Two points may share the same fraction along its length when there is
         * a move action within the Path.
         *
         * @param acceptableError The acceptable error for a line on the
         * Path. Typically this would be 0.5 so that
         * the error is less than half a pixel.
         * @return An array of components for points approximating the Path.
         */
        fun approximate(path: Path, @FloatRange(from = 0.0) acceptableError: Float): FloatArray {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return path.approximate(acceptableError)
            }
            require(acceptableError >= 0) { "acceptableError must be greater than or equal to 0" }
            // Measure the total length the whole pathData.
            val measureForTotalLength = PathMeasure(path, false)
            var totalLength = 0f
            // The sum of the previous contour plus the current one. Using the sum here
            // because we want to directly subtract from it later.
            val summedContourLengths: MutableList<Float> = ArrayList()
            summedContourLengths.add(0f)
            do {
                val pathLength = measureForTotalLength.length
                totalLength += pathLength
                summedContourLengths.add(totalLength)
            } while (measureForTotalLength.nextContour())

            // Now determine how many sample points we need, and the step for next sample.
            val pathMeasure = PathMeasure(path, false)
            val numPoints = Math.min(MAX_NUM_POINTS, (totalLength / acceptableError).toInt() + 1)
            val coords = FloatArray(NUM_COMPONENTS * numPoints)
            val position = FloatArray(2)
            var contourIndex = 0
            val step = totalLength / (numPoints - 1)
            var cumulativeDistance = 0f

            // For each sample point, determine whether we need to move on to next contour.
            // After we find the right contour, then sample it using the current distance value minus
            // the previously sampled contours' total length.
            for (i in 0 until numPoints) {
                // The cumulative distance traveled minus the total length of the previous contours
                // (not including the current contour).
                val contourDistance = cumulativeDistance - summedContourLengths[contourIndex]
                pathMeasure.getPosTan(contourDistance, position, null)
                coords[i * NUM_COMPONENTS + FRACTION_OFFSET] = cumulativeDistance / totalLength
                coords[i * NUM_COMPONENTS + X_OFFSET] =
                    position[0]
                coords[i * NUM_COMPONENTS + Y_OFFSET] =
                    position[1]
                cumulativeDistance = Math.min(cumulativeDistance + step, totalLength)

                // Using a while statement is necessary in the rare case where step is greater than
                // the length a path contour.
                while (summedContourLengths[contourIndex + 1] < cumulativeDistance) {
                    contourIndex++
                    pathMeasure.nextContour()
                }
            }
            coords[(numPoints - 1) * NUM_COMPONENTS + FRACTION_OFFSET] = 1f
            return coords
        }
    }

    companion object {
        // This governs how accurate the approximation of the Path is.
        private const val PRECISION = 0.002f
    }
}