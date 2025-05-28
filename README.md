# WeChat_RolePlay
基于WechatPadPro+文本大模型+SOVITS的微信聊天兼角色扮演机器人

## 前置准备
- 阿里云百炼开通APIKEY，用于后续调用文本模型(如果有本地部署的模型这一步可以调过)
- 下载GPT-Sovits整合包(需要保证显卡显存在4G以上),我这里使用版本是v2
- 从网上下载别人训练好的角色语音模型
- 角色参考音频（5秒内）
某大佬的sovits教程:[GPT-SoVITS指南](https://www.yuque.com/baicaigongchang1145haoyuangong/ib3g1e)

## 实现
### 一. 启动TTS模型
1.确保角色语音模型以及放到了对应文件夹中，如果语音模型是v1版本的就放在不带版本后缀的文件夹中
<img src="./media/img1.png">

2. 因为我们需要的是api调用语音模型，所以这里在控制台启动api_v2.py

<img src="./media/img9.png">

注意这里一定要用整合包的环境启动（~~我一开始用本地环境启动,结果就是下载了一大堆依赖,最后还是没能启动~~）

<img src="./media/img2.png">

语音模型相关api在该文件里有详细注释，主要包括了/tts生成音频文件，/set_gpt_weights切换GPT模型，/set_sovits_weights切换Sovits模型

### 二. 使用SpringAI调用大模型

1. 设置好对应角色的prompt，不会写的话可以直接查

<img src="./media/img3.png">

2. 调用文本模型的得到回复，将回复作为输入生成音频

回复内容大概长这样：

（視線を逸らし、エアポッドを触りながら）「…東の路地の子たち、最近警戒してるから。静かに行くんだよ。」

（）中的内容为动作或心理，只有「」中的内容是说话内容，所以要进行处理保留方括号内的内容modify_text
<img src="./media/img4.png">
生成音频（这里http参数从上到下分别是文本内容（要生成的语音内容），文本语言，参考音频地址，参考音频语言，参考音频文本，切分方式，并行数）

因为进行语音推理的时候是需要有参考的音频的，所以要提前准备，参考音频文本指音频的内容。
切分方式不用管和我一致就行，并行数应该是可以提高生成速度的没试过。

<img src="./media/img5.png">

总之最后的得到了文本内容以及音频地址（上面图片的txtAndAudioPathDTO）


### 三 wxAuto实现微信自动回复

没啥好说的，就是python的第三方库，原理好像是监控桌面上的微信，然后进行自动回复（期间不能使用键盘鼠标）

https://docs.wxauto.org/

逻辑很简单，将监听到的消息作为参数，调用springboot暴露的接口就行。

<img src="./media/img6.png">

### 四.效果展示
<img src="./media/img7.png">
<img src="./media/img8.png">


==============================================

**将wxauto改为wechatpadpro**
增加了问答助手功能（支持pid搜索图片）
<img src="./media/img10.png">
<img src="./media/img11.png">
复读机功能（其实就是我测试用的）
<img src="./media/img12.png">
角色扮演增加概率（70%）发送符合当前感情的图片，成功实现发送语音
前面使用的py库wxauto，更换成wechatPadPro https://github.com/luolin-ai/WeChatPadPro
（这里的对话其实可以只保留「」中的内容，但这里我还想看到动作内容，所以我保留了文本）
<img src="./media/img13.png">
<img src="./media/img14.png">