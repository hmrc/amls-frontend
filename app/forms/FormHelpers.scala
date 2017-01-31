package forms

import jto.validation.Path
import jto.validation.ValidationError

trait FormHelpers {

  trait MessageFilter {
    val messages: Seq[String]
  }

  case object StandardMessageFilter extends MessageFilter {
    override val messages = Seq(
      "error.expected.jodadate.format",
      "error.future.date"
    )
  }

  implicit val standardFilter = StandardMessageFilter

  implicit class InvalidFormExtensions(form: InvalidForm) {
    def withMessageFor(p: Path, message: String)(implicit exceptions: MessageFilter) = {
      form.errors.exists(f => f._1 == p && f._2.map(_.message).intersect(exceptions.messages).isEmpty) match {
        case true => InvalidForm(form.data, (form.errors filter (x => x._1 != p)) :+ (p, Seq(ValidationError(message))))
        case _ => form
      }
    }
  }

}
