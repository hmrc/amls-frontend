package utils

import play.twirl.api.Html

object Strings {

  implicit class ConsoleHelpers(s: String) {
    def in(colour: String) =s"$colour$s${Console.RESET}"
  }

  trait LineBreakConverter {
    def convert(input: String): String
  }

  implicit val defaultLineBreakConverter = new LineBreakConverter {
    override def convert(input: String) = input.replaceAll("""\s*\n\s*""", "</p><p>")
  }

  implicit class TextHelpers(s: String) {
    def convertLineBreaks(implicit converter: LineBreakConverter) = converter.convert(s)
    def convertLineBreaksH(implicit converter: LineBreakConverter) = Html(converter.convert(s))
    def paragraphize(implicit converter: LineBreakConverter) = s"<p>${converter.convert(s)}</p>"
    def paragraphizeH(implicit converter: LineBreakConverter) = Html(s"<p>${converter.convert(s)}</p>")
  }

}
