package utils

import _root_.forms.{InvalidForm, ValidForm, Form2}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded

class Form2Spec extends PlaySpec with MockitoSugar {

  case class Foobar(s: String, f: String)

  trait Fixture {

    implicit val formRule: Rule[UrlFormEncoded, Foobar] = From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "s").read[String] and
          (__ \ "f").read[String]
        )(Foobar.apply _)
    }

    implicit val formWrites: Write[Foobar, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "s").write[String] and
          (__ \ "f").write[String]
        )(unlift(Foobar.unapply _))
    }

    val model = Foobar("foo", "bar")

    val data = Map(
      "s" -> Seq("foo"),
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
        (Path \ "s") -> Seq(ValidationError("error.required")),
        (Path \ "f") -> Seq(ValidationError("error.required"))
      )

      Form2[Foobar](Map.empty[String, Seq[String]]) must
        be(InvalidForm(Map.empty, errors))
    }

    "return data for a field when given a path" in new Fixture {

      Form2[Foobar](model).apply(Path \ "s") must
        be(Some("foo"))
    }
  }

  "InvalidForm" must {

    "return only the errors for the specified field" in new Fixture {

      val errors = Seq(
        ValidationError("error.required")
      )

      Form2[Foobar](Map.empty[String, Seq[String]]).errors(Path \ "s") must
        be(errors)
    }
  }
}
