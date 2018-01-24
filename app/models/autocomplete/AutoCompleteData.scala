/*
 * Copyright 2018 HM Revenue & Customs
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

package models.autocomplete

import javax.inject.Inject

import play.api.Environment
import play.api.libs.json.Json

import scala.io.Source

trait AutoCompleteData {
  def fetch: Option[Seq[NameValuePair]]
}

// $COVERAGE-OFF$
class ResourceFileAutoCompleteData @Inject()(env: Environment) extends AutoCompleteData {
  override def fetch: Option[Seq[NameValuePair]] = env.resourceAsStream("public/autocomplete/location-autocomplete-canonical-list.json") map { stream =>
    Json.parse(Source.fromInputStream(stream).mkString).as[Seq[NameValuePair]]
  }
}
// $COVERAGE-ON$
