package org.emergent.gittle.maven.extension;

public class ExtensionUtilTest {

  // @Test
  // void toXml_Conf() throws Exception {
  //   Xpp3Dom expected = Xpp3DomBuilder.build(new StringReader("""
  //       <configuration>
  //         <newVersion>0.1.2</newVersion>
  //         <releaseBranchRegex>^(release|stable)$</releaseBranchRegex>
  //         <tagNameRegex>v?([0-9]+\\.[0-9]+\\.[0-9]+)</tagNameRegex>
  //         <versionPattern>%t(-%B)(-%C)(-%S)(+%H)(.%D)</versionPattern>
  //       </configuration>
  //       """));
  //   assertThat(ExtensionUtil.toXml(getConf())).isNotNull().isEqualTo(expected);
  // }

  // private static Config getConf() {
  //   return Config.builder()
  //       .setNewVersion("0.1.2")
  //       .setReleaseBranchRegex("^(release|stable)$")
  //       .setTagNameRegex("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
  //       .setVersionPattern("%t(-%B)(-%C)(-%S)(+%H)(.%D)")
  //       .build();
  // }

}
