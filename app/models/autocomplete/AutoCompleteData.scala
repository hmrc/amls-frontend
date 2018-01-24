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

import java.io.InputStream
import javax.inject.Inject

import com.google.inject.ImplementedBy
import play.api.Environment
import play.api.libs.json.Json

import scala.io.Source

@ImplementedBy(classOf[GovUkAutoCompleteData])
trait AutoCompleteData {
  def fetch: Option[Seq[NameValuePair]]
}

class GovUkAutoCompleteData @Inject()(env: Environment) extends AutoCompleteData {
  lazy val countryCodes = models.countries.map(_.code.toUpperCase()).toSet

  private val resourcePath = "public/autocomplete/location-autocomplete-canonical-list.json"

  private def stripCode(value: String): String = value.substring(value.indexOf(":") + 1)

  private def isEncoded(value: String): Boolean = value.indexOf(":") > -1

  private def getJson: Option[InputStream] = env.resourceAsStream(resourcePath)

  /**
    * This implementation attempts to read the canonical list of countries and territories from a JSON file. In doing that,
    * it manipulates the country code so that only the code itself is returned (the JSON defines country code format as 'country:GB' or 'territory:GB').
    * Furthermore, it strips out those codes that do not appear in models.countries, as otherwise the application would fail schema validation when it
    * was submitted.
    */
  override def fetch: Option[Seq[NameValuePair]] = getJson map { stream =>
    Json.parse(Source.fromInputStream(stream).mkString).as[Seq[NameValuePair]] map {
      case n@NameValuePair(_, value) if isEncoded(value) => n.copy(value = stripCode(value))
      case n => n
    } collect {
      case n if countryCodes.contains(n.value.toUpperCase()) => n
    }
  }
}
