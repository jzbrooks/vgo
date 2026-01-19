package com.jzbrooks.vgo.core.util.math

/**
 * Computes the convex hull of a set of points using Andrew's monotone chain algorithm.
 * @return list of points forming the convex hull in counter-clockwise order
 */
fun convexHull(points: List<Point>): List<Point> {
    if (points.size < 3) return points

    val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))

    // Build lower hull
    val lower = mutableListOf<Point>()
    for (p in sorted) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
            lower.removeLast()
        }
        lower.add(p)
    }

    // Build upper hull
    val upper = mutableListOf<Point>()
    for (p in sorted.reversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
            upper.removeLast()
        }
        upper.add(p)
    }

    // Remove last point of each half because it's repeated
    lower.removeLast()
    upper.removeLast()

    return lower + upper
}

/**
 * 2D cross-product of vectors OA and OB where O is the origin.
 * Returns positive if counter-clockwise, negative if clockwise, zero if collinear.
 */
private fun cross(
    o: Point,
    a: Point,
    b: Point,
): Float = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

/**
 * Determines if two convex polygons intersect using the GJK algorithm.
 * The polygons are represented as lists of vertices in counter-clockwise order.
 *
 * @param firstHull The first convex polygon (convex hull)
 * @param secondHull The second convex polygon (convex hull)
 * @return true if the polygons intersect, false otherwise
 */
fun intersects(
    firstHull: List<Point>,
    secondHull: List<Point>,
): Boolean {
    if (firstHull.isEmpty() || secondHull.isEmpty()) return false
    if (firstHull.size == 1 && secondHull.size == 1) {
        return firstHull[0].isApproximately(secondHull[0])
    }

    // Initial direction
    var direction = Point(1f, 0f)

    // Get first support point
    var a = support(firstHull, secondHull, direction)
    val simplex = mutableListOf(a)

    // New direction towards the origin
    direction = -a

    var iterations = 0
    val maxIterations = 100

    while (iterations < maxIterations) {
        iterations++

        a = support(firstHull, secondHull, direction)

        // If the new point didn't pass the origin, no intersection
        if (a.dot(direction) < 0) {
            return false
        }

        simplex.add(a)

        // Check if simplex contains origin and update direction
        if (handleSimplex(simplex) { direction = it }) {
            return true
        }
    }

    // If we exhausted iterations, assume no intersection
    return false
}

/**
 * Computes the support point of the Minkowski difference A - B in the given direction.
 */
private fun support(
    hullA: List<Point>,
    hullB: List<Point>,
    direction: Point,
): Point {
    val furthestA = furthestPoint(hullA, direction)
    val furthestB = furthestPoint(hullB, -direction)
    return furthestA - furthestB
}

/**
 * Finds the point in the hull that is furthest in the given direction.
 */
private fun furthestPoint(
    hull: List<Point>,
    direction: Point,
): Point {
    var maxDot = Float.NEGATIVE_INFINITY
    var furthest = hull[0]

    for (point in hull) {
        val dotProduct = point.dot(direction)
        if (dotProduct > maxDot) {
            maxDot = dotProduct
            furthest = point
        }
    }

    return furthest
}

/**
 * Processes the simplex and updates the search direction.
 * Returns true if the origin is contained in the simplex.
 */
private inline fun handleSimplex(
    simplex: MutableList<Point>,
    setDirection: (Point) -> Unit,
): Boolean =
    when (simplex.size) {
        2 -> handleLine(simplex, setDirection)
        3 -> handleTriangle(simplex, setDirection)
        else -> false
    }

/**
 * Handles a line simplex (2 points).
 */
private inline fun handleLine(
    simplex: MutableList<Point>,
    setDirection: (Point) -> Unit,
): Boolean {
    val a = simplex[1]
    val b = simplex[0]
    val ab = b - a
    val ao = -a

    if (ab.dot(ao) > 0) {
        // Origin is in the region of the line segment
        setDirection(tripleProduct(ab, ao, ab))
    } else {
        // Origin is past A, remove B
        simplex.clear()
        simplex.add(a)
        setDirection(ao)
    }

    return false
}

/**
 * Handles a triangle simplex (3 points).
 */
private inline fun handleTriangle(
    simplex: MutableList<Point>,
    setDirection: (Point) -> Unit,
): Boolean {
    val a = simplex[2]
    val b = simplex[1]
    val c = simplex[0]

    val ab = b - a
    val ac = c - a
    val ao = -a

    val abPerp = tripleProduct(ac, ab, ab)
    val acPerp = tripleProduct(ab, ac, ac)

    if (abPerp.dot(ao) > 0) {
        // Origin is outside the triangle on the AB edge side
        simplex.removeAt(0) // Remove C
        setDirection(abPerp)
        return false
    }

    if (acPerp.dot(ao) > 0) {
        // Origin is outside the triangle on the AC edge side
        simplex.removeAt(1) // Remove B
        setDirection(acPerp)
        return false
    }

    // Origin is inside the triangle
    return true
}

/**
 * Computes the triple product (A x B) x C, which gives a vector perpendicular to C
 * in the plane formed by A and B, pointing towards the origin.
 */
private fun tripleProduct(
    a: Point,
    b: Point,
    c: Point,
): Point {
    // In 2D, (A x B) x C = B * (A · C) - A * (B · C)
    val ac = a.dot(c)
    val bc = b.dot(c)
    return Point(b.x * ac - a.x * bc, b.y * ac - a.y * bc)
}
