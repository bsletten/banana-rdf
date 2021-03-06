package org.w3.banana

import org.scalatest._
import org.scalatest.matchers.MustMatchers
import java.io._
import org.scalatest.EitherValues._

import scalaz._
import scalaz.Validation._

abstract class DieselGraphExplorationTest[Rdf <: RDF]()(implicit diesel: Diesel[Rdf])
    extends WordSpec with MustMatchers {

  import org.scalatest.matchers.{ BeMatcher, MatchResult }
  import diesel._
  import ops._

  val betehess: PointedGraph[Rdf] = (
    uri("http://bertails.org/#betehess").a(foaf.Person)
    -- foaf.name ->- "Alexandre".lang("fr")
    -- foaf.name ->- "Alexander".lang("en")
    -- foaf.age ->- 29
    -- foaf("foo") ->- List(1, 2, 3)
    -- foaf.knows ->- (
      uri("http://bblfish.net/#hjs").a(foaf.Person)
      -- foaf.name ->- "Henry Story"
      -- foaf.currentProject ->- uri("http://webid.info/")
    )
  )

  "'/' method must traverse the graph" in {

    val name = betehess / foaf.name

    name.head.pointer must be(LangLiteral("Alexandre", Lang("fr")))

  }

  "'/' method must work with uris and bnodes" in {

    val name = betehess / foaf.knows / foaf.name

    name.head.pointer must be(TypedLiteral("Henry Story"))

  }

  "we must be able to project nodes to Scala types" in {

    (betehess / foaf.age).as[Int] must be(Success(29))

    (betehess / foaf.knows / foaf.name).as[String] must be(Success("Henry Story"))

  }

  "betehess should have three predicates: foaf:name foaf:age foaf:knows" in {

    val predicates = betehess.predicates.toList
    List(foaf.name, foaf.age, foaf.knows) foreach { p => predicates must contain(p) }

  }

  "we must be able to get rdf lists" in {

    (betehess / foaf("foo")).as[List[Int]] must be(Success(List(1, 2, 3)))

  }

  "we must be able to optionally get objects" in {

    (betehess / foaf.age).asOption[Int] must be(Success(Some(29)))

    (betehess / foaf.age).asOption[String] must be('failure)

    (betehess / foaf("unknown")).asOption[Int] must be(Success(None))

  }

  "asking for one (or exactly one) node when there is none must fail" in {

    (betehess / foaf("unknown")).takeOnePointedGraph must be('failure)

    (betehess / foaf("unknown")).exactlyOnePointedGraph must be('failure)

  }

  "asking for exactly one node when there are more than one must fail" in {

    (betehess / foaf.name).exactlyOnePointedGraph must be('failure)

  }

  "asking for one node when there is at least one must be a success" in {

    (betehess / foaf.name).takeOnePointedGraph must be('success)

    (betehess / foaf.age).takeOnePointedGraph must be('success)

  }

  "asking for exactly one pointed graph when there is none must fail" in {

    (betehess / foaf("unknown")).exactlyOnePointedGraph must be('failure)

  }

  "asking for exactly one pointed graph when there are more than one must fail" in {

    (betehess / foaf.name).exactlyOnePointedGraph must be('failure)

  }

  "getAllInstancesOf must give all instances of a given class" in {

    val persons = betehess.graph.getAllInstancesOf(foaf.Person).nodes

    persons.toSet must be === (Set(uri("http://bertails.org/#betehess"), uri("http://bblfish.net/#hjs")))

  }

  "isA must test if a node belongs to a class" in {

    betehess.isA(foaf.Person) must be(true)

    betehess.isA(foaf("SomethingElse")) must be(false)

  }

}
