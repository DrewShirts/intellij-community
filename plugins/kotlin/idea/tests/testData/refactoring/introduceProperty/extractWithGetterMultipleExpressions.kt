// EXTRACTION_TARGET: property with getter
class A {
    fun foo(): Int {
        <selection>val a = 1 + 2
        val b = a*2</selection>
        val c = b - 1
    }
}

