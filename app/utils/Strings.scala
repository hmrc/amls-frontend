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
    override def convert(input: String) = input.replace("\n", "</p><p>")
  }

  implicit class TextHelpers(s: String) {
    def convertLineBreaks(implicit converter: LineBreakConverter) = converter.convert(s)
    def convertLinkBreaksH(implicit converter: LineBreakConverter) = Html(converter.convert(s))
  }

}
