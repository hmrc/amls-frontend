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

package utils

import _root_.forms.{ValidField, InvalidForm, ValidForm, Form2}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation._
import jto.validation.forms.UrlFormEncoded

class Form2Spec extends PlaySpec with MockitoSugar {

  case class Foobar(s: String, f: String)

  trait Fixture {

    implicit val formRule: Rule[UrlFormEncoded, Foobar] = From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (
        (__ \ "services").read[String] ~
          (__ \ "f").read[String]
        )(Foobar.apply _)
    }

    implicit val formWrites: Write[Foobar, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
      import jto.validation.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "services").write[String] ~
          (__ \ "f").write[String]
        )(unlift(Foobar.unapply _))
    }

    val model = Foobar("foo", "bar")

    val data = Map(
      "services" -> Seq("foo"),
      "f" -> Seq("bar")
    )
  }

  "Form2" must {

    "return a ValidForm when a valid model is passed" in new Fixture {

      Form2[Foobar](model) must
        be(ValidForm(data, model))
    }

    "return a Valid when valid UrlFormEncoded data is passed" in new Fixture {

      Form2[Foobar](data) must
        be(ValidForm(data, model))
    }

    "return an InvalidForm when invalid UrlFormEncoded data is passed" in new Fixture {

      val errors = Seq(
        (Path \ "services") -> Seq(ValidationError("error.required")),
        (Path \ "f") -> Seq(ValidationError("error.required"))
      )

      Form2[Foobar](Map.empty[String, Seq[String]]) must
        be(InvalidForm(Map.empty, errors))
    }

    "return data for a field when given a path" in new Fixture {

      Form2[Foobar](model).apply(Path \ "services") must
        be(ValidField(Path \ "services", Seq("foo")))
    }
  }

  "InvalidForm" must {

    "return only the errors for the specified field" in new Fixture {

      val errors = Seq(
        ValidationError("error.required")
      )

      Form2[Foobar](Map.empty[String, Seq[String]]).errors(Path \ "services") must
        be(errors)
    }
  }
}
