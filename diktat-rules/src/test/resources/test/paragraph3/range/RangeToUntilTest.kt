package test.paragraph3.range

class A {
    fun foo() {
        for (i in 1..4) print(i)
        for (i in 4 downTo 1) print(i)
        for (i in 1 until 4) print(i)
        for (i in 1..4 step 2) print(i)
        for (i in 4 downTo 1 step 3) print(i)
        if (6 in (1..10) && true) {}
        for (i in 1..(4 - 1)) print(i)
        for (i in 1..(b - 1)) print(i)
        for (i in ((1 .. ((4 - 1))))) print(i)
    }
}
