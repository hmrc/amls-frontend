/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      val first = PreviousName(Some("oldfirst"), None, None, Some(new LocalDate(1990, 2, 24)))
      val middle = PreviousName(None, Some("oldmiddle"), None, Some(new LocalDate(1990, 2, 24)))
      val last = PreviousName(None, None, Some("oldlast"), Some(new LocalDate(1990, 2, 24)))

      val personName = PersonName("First", Some("Middle"), "Last", None, None)

      first.formattedPreviousName(personName) must be ("oldfirst Middle Last")
      middle.formattedPreviousName(personName) must be ("First oldmiddle Last")
      last.formattedPreviousName(personName) must be ("First Middle oldlast")

    }

    "successfully validate with all fields" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq("oldFirst"),
        "middleName" -> Seq("oldMiddle"),
        "lastName" -> Seq("oldLast"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Valid(PreviousName(
            firstName = Some("oldFirst"),
            middleName = Some("oldMiddle"),
            lastName = Some("oldLast"),
            // scalastyle:off magic.number
            date = Some(new LocalDate(1990, 2, 24))
          )
        ))
    }



    "successfully validate with just firstName" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq("oldFirst"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Valid(PreviousName(
            firstName = Some("oldFirst"),
            middleName = None,
            lastName = None,
            // scalastyle:off magic.number
            date = Some(new LocalDate(1990, 2, 24))
          )
        ))
    }

    "successfully validate with just middleName" in {

      val data: UrlFormEncoded = Map(
        "middleName" -> Seq("oldMiddle"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Valid(PreviousName(
            firstName = None,
            middleName = Some("oldMiddle"),
            lastName = None,
            // scalastyle:off magic.number
            date = Some(new LocalDate(1990, 2, 24))
          )
        ))
    }

    "successfully validate with just lastName" in {

      val data: UrlFormEncoded = Map(
        "lastName" -> Seq("oldLast"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Valid(PreviousName(
            firstName = None,
            middleName = None,
            lastName = Some("oldLast"),
            // scalastyle:off magic.number
            date = Some(new LocalDate(1990, 2, 24))
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
          Invalid(Seq(
            Path -> Seq(ValidationError("error.rp.previous.invalid"))
          ))
        )
    }

    "fail to validate with missing date" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq(""),
        "middleName" -> Seq(""),
        "lastName" -> Seq("oldLast"),
        "date.day" -> Seq(""),
        "date.month" -> Seq(""),
        "date.year" -> Seq("")
      )

      implicitly[Rule[UrlFormEncoded, PreviousName]].validate(data) must
        equal(
          Invalid(Seq(
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
          Invalid(Seq(
            (Path) -> Seq(ValidationError("error.rp.previous.invalid")),
            (Path \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))))
        )
    }

    "correctly serialise" in {

      val data: UrlFormEncoded = Map(
        "firstName" -> Seq("oldFirst"),
        "middleName" -> Seq("oldMiddle"),
        "lastName" -> Seq("oldLast"),
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      val model = PreviousName(
        firstName = Some("oldFirst"),
        middleName = Some("oldMiddle"),
        lastName = Some("oldLast"),
        // scalastyle:off magic.number
        date = Some(new LocalDate(1990, 2, 24))
      )

      implicitly[Write[PreviousName, UrlFormEncoded]].writes(model) mustEqual data
    }
  }
}
