package services

import javax.inject.Inject

import models.autocomplete.{AutoCompleteData, Location}

class AutoCompleteService @Inject()(data: AutoCompleteData) {
  lazy val getLocations: Option[Seq[Location]] = data.fetch
}
