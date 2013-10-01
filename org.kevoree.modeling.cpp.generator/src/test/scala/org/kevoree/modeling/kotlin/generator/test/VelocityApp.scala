/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * Fouquet Francois
 * Nain Gregory
 */
/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * 	Fouquet Francois
 * 	Nain Gregory
 */
package org.kevoree.modeling.kotlin.generator.test

import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.apache.velocity.VelocityContext
import java.io.StringWriter

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 18/03/13
 * Time: 11:10
 */
object VelocityApp extends App {

  val ve = new VelocityEngine()
  ve.setProperty("file.resource.loader.class", classOf[ClasspathResourceLoader].getName())
  ve.init()

  val template = ve.getTemplate( "Tester.vm" );
  val ctx = new VelocityContext()
  ctx.put("formatedFactoryName","MyFactoryName")
  ctx.put("packElem","MyContainerPackage")

  val names = java.util.Arrays.asList("L1","L2","L3","L4","L5","L6","L7")
  ctx.put("customerList",names)


  val writer = new StringWriter()
  template.merge(ctx,writer)

  println(writer.toString)


}
