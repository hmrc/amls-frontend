package models.responsiblepeople

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError

class PreviousNameSpec extends PlaySpec {

  "PreviousName" must {

    "have the formattedPreviousName function correctly return the value" in {

      // scalastyle:off magic.number
      val first = PreviousName(Some("Matt"), None, None, new LocalDate(1990, 2, 24))
      val middle = PreviousName(None, Some("Matt"), None, new LocalDate(1990, 2, 24))
      val last = PreviousName(None, None, Some("Matt"), new LocalDate(1990, 2, 24))

      val personName = PersonName("John", Some("Paul"), "Smith", None, None)

      first.formattedPreviousName(personName) must be ("Matt Paul Smith")
      middle.formattedPreviousName(personName) must be ("John Matt Smith")
      last.formattedPreviousName(personName) must be ("John Paul Matt")

    }

    "successfully validate with all fields" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq("Marty"),
        "middleName" -> Seq("Mc"),
        "lastName" -> Seq("Fly"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Success(PreviousName(
            firstName = Some("Marty"),
            middleName = Some("Mc"),
            lastName = Some("Fly"),
            // scalastyle:off magic.number
            date = new LocalDate(1990, 2, 24)
          )
        ))
    }



    "successfully validate with just firstName" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq("Marty"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Success(PreviousName(
            firstName = Some("Marty"),
            middleName = None,
            lastName = None,
            // scalastyle:off magic.number
            date = new LocalDate(1990, 2, 24)
          )
        ))
    }

    "successfully validate with just middleName" in {

      val data: UrlFormEncoded = Map(
        "middleName" -> Seq("Mc"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Success(PreviousName(
            firstName = None,
            middleName = Some("Mc"),
            lastName = None,
            // scalastyle:off magic.number
            date = new LocalDate(1990, 2, 24)
          )
        ))
    }

    "successfully validate with just lastName" in {

      val data: UrlFormEncoded = Map(
        "lastName" -> Seq("Fly"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Success(PreviousName(
            firstName = None,
            middleName = None,
            lastName = Some("Fly"),
            // scalastyle:off magic.number
            date = new LocalDate(1990, 2, 24)
          )
        ))
    }

    "fail to validate with no names" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq(""),
        "middleName" -> Seq(""),
        "lastName" -> Seq(""),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Failure(Seq(
            Path -> Seq(ValidationError("error.rp.previous.invalid"))
          ))
        )
    }

    "fail to validate with missing date" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq(""),
        "middleName" -> Seq(""),
        "lastName" -> Seq("Fly"),
        "date.day" -> Seq(""),
        "date.month" -> Seq(""),
        "date.year" -> Seq("")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Failure(Seq(
            (Path \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
          ))
        )
    }

    "fail to validate with both" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq(""),
        "middleName" -> Seq(""),
        "lastName" -> Seq(""),
        "date.day" -> Seq(""),
        "date.month" -> Seq(""),
        "date.year" -> Seq("")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Failure(Seq(
            (Path) -> Seq(ValidationError("error.rp.previous.invalid")),
            (Path \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))))
        )
    }

    "correctly serialise" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq("Marty"),
        "middleName" -> Seq("Mc"),
        "lastName" -> Seq("Fly"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      val model = PreviousName(
        firstName = Some("Marty"),
        middleName = Some("Mc"),
        lastName = Some("Fly"),
        // scalastyle:off magic.number
        date = new LocalDate(1990, 2, 24)
      )

      implicitly[Write[PreviousName, UrlFormEncoded]].writes(model) mustEqual data
    }
  }
}
