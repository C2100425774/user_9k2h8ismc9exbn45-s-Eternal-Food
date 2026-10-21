# libs/

这里放**编译期**第三方依赖，不入版本库（见根目录 `.gitignore`）。

## 车万女仆（Touhou Little Maid）

本模组对 TLM 只是**软依赖**：装了 TLM 才会有女仆联动（驯服、零食柜、卖萌动画），
但编译时必须要它的类才能通过，所以需要在本地放一份：

1. 打开 <https://modrinth.com/mod/touhou-little-maid/versions>
2. 下载 `1.20.1` + `Forge` + `1.5.3` 那个 jar
3. 重命名为 `touhoulittlemaid-1.5.3-forge+mc1.20.1.jar`
4. 放到本目录下

放好之后 `./gradlew build` 即可。这个 jar 不会被复制进本模组的产物里。