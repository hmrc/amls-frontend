package forms

import play.twirl.api.{HtmlFormat, Html}

object FormHelpers {

  def id(name: String): String =
    name.replace('.', '-')

  def classes(pres: Presentation): Html = {
    pres.classes.mkString(" ") match {
      case "" => HtmlFormat.empty
      case classes => Html(s"""class="$classes"""")
    }
  }

  case class Presentation(classes: Seq[String])

  object Defaults {

    val label = Presentation(classes = Seq("form-label"))

    val legend = Presentation(classes = Seq("heading-small"))

    val legendInvisible = Presentation(classes = Seq("visibly-hidden"))

    val hint = Presentation(classes = Seq("form-hint"))

    val input = Presentation(classes = Seq("form-control"))

    val field = Presentation(classes = Seq("form-group"))

    val fieldset = Presentation(classes = Seq("panel-indent"))

    val radioGroup = Presentation(classes = Seq(""))
  }
}
