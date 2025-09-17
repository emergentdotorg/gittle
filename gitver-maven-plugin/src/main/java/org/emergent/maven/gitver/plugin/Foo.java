package org.emergent.maven.gitver.plugin;

import com.google.inject.Guice;
import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

// let's create some simple Swing tabs...

public class Foo {

    static abstract class AbstractTab extends JPanel {}

    // Sisu will spot any @Named components

    @Named("One")
    static class ButtonTab extends AbstractTab {
        ButtonTab() {
            add(new JButton("Button"));
        }
    }

    @Named("Two")
    static class CheckBoxTab extends AbstractTab {
        CheckBoxTab() {
            add(new JCheckBox("CheckBox"));
        }
    }

    @Named("Three")
    static class RadioButtonTab extends AbstractTab {
        RadioButtonTab() {
            ButtonGroup group = new ButtonGroup();
            group.add(new JRadioButton("+1"));
            group.add(new JRadioButton("0"));
            group.add(new JRadioButton("-1"));
            Enumeration<AbstractButton> e = group.getElements();
            while (e.hasMoreElements()) {
                add(e.nextElement());
            }
        }
    }

    // Sisu will launch @EagerSingleton's on startup

    @Named
    @org.eclipse.sisu.EagerSingleton
    static class Example implements Runnable {
        JTabbedPane pane = new JTabbedPane();

        // Sisu spots we want a map of tabs and wires it up for us

        @Inject
        Example(Map<String, AbstractTab> tabs) {

            for (Map.Entry<String, AbstractTab> t : tabs.entrySet()) {
                pane.addTab(t.getKey(), t.getValue());
            }

            SwingUtilities.invokeLater(this);
        }

        public void run() {
            JFrame frame = new JFrame("Sisu 5-minute demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(pane, BorderLayout.CENTER);
            frame.setLocation(100, 50);
            frame.setSize(300, 200);
            frame.setVisible(true);
        }

        public static void main(String[] args) {
            ClassLoader classloader = Example.class.getClassLoader();
            Guice.createInjector(new WireModule(                       // auto-wires unresolved dependencies
              new SpaceModule(                     // scans and binds @Named components
                new URLClassSpace(classloader)    // abstracts class/resource finding
              )));
        }
    }

}