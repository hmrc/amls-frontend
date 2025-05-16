/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import com.vladsch.flexmark.ast.{BulletList, Heading, Paragraph}
import com.vladsch.flexmark.html.HtmlRenderer.HtmlRendererExtension
import com.vladsch.flexmark.html.renderer.{AttributablePart, LinkResolverContext}
import com.vladsch.flexmark.html.{AttributeProvider, AttributeProviderFactory, HtmlRenderer}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.{MutableDataHolder, MutableDataSet}
import com.vladsch.flexmark.util.html.MutableAttributes
import com.vladsch.flexmark.util.misc.Extension

import java.util

object CustomAttributeProvider {

  object CustomExtension extends HtmlRendererExtension {
    override def rendererOptions(mutableDataHolder: MutableDataHolder): Unit = {}

    override def extend(builder: HtmlRenderer.Builder, s: String): Unit =
      builder.attributeProviderFactory(new AttributeProviderFactory {
        override def getAfterDependents = null

        override def getBeforeDependents = null

        override def affectsGlobalScope(): Boolean = true

        override def apply(context: LinkResolverContext): AttributeProvider = CustomAttributeProvider
      })
  }

  object CustomAttributeProvider extends AttributeProvider {
    override def setAttributes(node: Node, part: AttributablePart, attributes: MutableAttributes): Unit =
      if (node.isInstanceOf[BulletList]) {
        attributes.toMutable.replaceValue("class", "govuk-list govuk-list--bullet")
      } else if (node.isInstanceOf[Paragraph]) {
        attributes.toMutable.replaceValue("class", "govuk-body")
      } else if (node.isInstanceOf[Heading]) {
        attributes.toMutable.replaceValue("class", "govuk-heading-l")
      }

  }

  def commonMark(rawEtmp: String): String = {
    val markdown = rawEtmp.replace("<P>", "\n").replace("</P>", "\n\n").replace("<tab> ", " ")
    val options  = new MutableDataSet
    options.set[util.Collection[Extension]](Parser.EXTENSIONS, util.Arrays.asList(CustomExtension))
    options.set(HtmlRenderer.SOFT_BREAK, "<br/>")
    val parser   = Parser.builder(options).build
    val document = parser.parse(markdown)
    val renderer = HtmlRenderer.builder(options).build
    val html     = renderer.render(document)
    html
  }
}
