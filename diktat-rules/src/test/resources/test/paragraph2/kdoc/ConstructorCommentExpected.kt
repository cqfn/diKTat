package test.paragraph2.kdoc

/**
 * Simple class
 * @property name  should replace
*/
class A constructor(
        name: String
)

/**
 * @property age age
 * @property lastName Ivanov
*/
class A(
        val age: Int,
        val lastName: String
) {

}

/**
 * @property Age age
 * @property age 13
*/
class B {
    constructor(
            age: Int,
            name: String
    ){}
}

class Out{
    /**
     * Inner class
     * @property name Jack
*/
    class In(
            name: String
    ){}
}
