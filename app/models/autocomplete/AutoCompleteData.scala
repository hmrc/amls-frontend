package models.autocomplete

import javax.inject.Inject

import play.api.Environment
import play.api.libs.json.Json

import scala.io.Source

trait AutoCompleteData {
  def fetch: Option[Seq[Location]]
}

// $COVERAGE-OFF$
class ResourceFileAutoCompleteData @Inject()(env: Environment) extends AutoCompleteData {
  override def fetch: Option[Seq[Location]] = env.resourceAsStream("public/autocomplete/location-autocomplete-canonical-list.json") map { stream =>
    Json.parse(Source.fromInputStream(stream).mkString).as[Seq[Location]]
  }
}
// $COVERAGE-ON$
