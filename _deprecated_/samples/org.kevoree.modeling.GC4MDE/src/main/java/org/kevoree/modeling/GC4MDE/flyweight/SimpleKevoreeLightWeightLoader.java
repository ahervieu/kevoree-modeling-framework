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
package org.kevoree.modeling.GC4MDE.flyweight;

import org.kevoree.ContainerRoot;
import org.kevoree.impl.FlyweightKevoreeFactory;
import org.kevoree.loader.XMIModelLoader;
import org.kevoree.modeling.GC4MDE.SimpleLoopApp;

import java.util.List;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 15/04/13
 * Time: 23:29
 */
public class SimpleKevoreeLightWeightLoader {

    public static void main(String[] args){

        String content = buildTestRes();
        XMIModelLoader loader = new XMIModelLoader();

        long before = System.currentTimeMillis();
        for (int i = 0; i < 25000; i++) {
            List<ContainerRoot> models = loader.loadModelFromString(content);
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
        }
        long after = System.currentTimeMillis();
        System.out.println("Time spent : " + (after - before) + " ms ");
        System.out.println("Time spent per model (avg) : " + ((after - before) / 25000) + " ms ");
    }

    public static String buildTestRes(){
        return new Scanner(SimpleLoopApp.class.getClassLoader().getResourceAsStream("bootKloudNode1.kev"),"UTF-8").useDelimiter("\\A").next();
    }

}
