/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.notebook;

import com.google.common.collect.Maps;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.display.AngularObject;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.helium.HeliumPackage;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.apache.zeppelin.user.Credentials;
import org.apache.zeppelin.user.UserCredentials;
import org.apache.zeppelin.display.GUI;
import org.apache.zeppelin.display.Input;
import org.apache.zeppelin.interpreter.*;
import org.apache.zeppelin.interpreter.Interpreter.FormType;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.resource.ResourcePool;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.scheduler.JobListener;
import org.apache.zeppelin.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import com.google.common.annotations.VisibleForTesting;

/**
 * Paragraph is a representation of an execution unit.
 * 段落是执行部件的表现形式，也就是执行的最小单元
 *
 * 是执行部件的表现形式，也就是执行的最小单元，对应在web上我们写的代码，我将它比作日记本的每一行字
     作用： 1:获取代码文本，分离出用户设置的解析器例如%sql，得到相对应解释器的信息，解析用户写的代码段
           2:继承Jobl类，代码执行，控制代码过程是开始还是中止，对应用户在web页面上的操作
           3:执行结果的返回
           4：代码中变量的查找以及替换
 */
public class Paragraph extends Job implements Serializable, Cloneable {
  /*
  job是一个抽象类，可以控制job的中断，运行等对应web端的控制，即Paragraphy的执行状态首job控制
   */

  private static final long serialVersionUID = -6328572073497992016L;
  /*
  实现序列化的UID

 */

  private static Logger logger = LoggerFactory.getLogger(Paragraph.class);
  /*
  是 Interpreter 接口的工厂类，
  各种 Interpreter， 其实这个类创建出来的全部都是主进程中的代理）
  都是从这个类中创造出来的，换句话说，
  这个类专门为独立的解释器进程创建代理对象
   */
  private transient InterpreterFactory factory;
  private transient InterpreterSettingManager interpreterSettingManager;
  private transient Note note;
  /*
  身份验证，安全问题
   */
  private transient AuthenticationInfo authenticationInfo;
  /*
userParagraphMap里面存了key：用户名，value：paragraph对象，可以获得user和其对应的paragraph内容
   */
  private transient Map<String, Paragraph> userParagraphMap = Maps.newHashMap(); // personalized

  String title;
  String text;//获取代码文本内容
  String user;//获取用户
  Date dateUpdated;//更新时间
  private Map<String, Object> config; // paragraph configs like isOpen, colWidth, etc
  /**
   * GUI 表单和参数变量设置
   */
  public GUI settings;          // form and parameter settings


  // since zeppelin-0.7.0, zeppelin stores multiple results of the paragraph
  // see ZEPPELIN-212
  Object results;
  /**
   * 对于以后版本中的note兼容
   */
  // For backward compatibility of note.json format after ZEPPELIN-212
  Object result;

  /**
   * Applicaiton states in this paragraph
   * 应用程序的状态loading；unloded;loaded;error
   * 把每个应用的状态存到了LIST中
   */
  private final List<ApplicationState> apps = new LinkedList<>();

  @VisibleForTesting
  Paragraph() {
    super(generateId(), null);
    config = new HashMap<>();
    settings = new GUI();//从表单中获得
  }

  /**
   * 传入一些指定参数配置
   * @param paragraphId
   * @param note
   * @param listener
   * @param factory
   * @param interpreterSettingManager
   */
  public Paragraph(String paragraphId, Note note, JobListener listener,
      InterpreterFactory factory, InterpreterSettingManager interpreterSettingManager) {
    super(paragraphId, generateId(), listener);
    this.note = note;
    this.factory = factory;
    this.interpreterSettingManager = interpreterSettingManager;
    title = null;
    text = null;
    authenticationInfo = null;
    user = null;
    dateUpdated = null;
    settings = new GUI();
    config = new HashMap<>();
  }

  public Paragraph(Note note, JobListener listener, InterpreterFactory factory,
      InterpreterSettingManager interpreterSettingManager) {
    super(generateId(), listener);
    this.note = note;
    this.factory = factory;
    this.interpreterSettingManager = interpreterSettingManager;
    title = null;
    text = null;
    authenticationInfo = null;
    dateUpdated = null;
    settings = new GUI();
    config = new HashMap<>();
  }
/*
生成一个id paragraph_+时间戳+随机数作为paragrathID作为识别
 */
  private static String generateId() {
    return "paragraph_" + System.currentTimeMillis() + "_" + new Random(System.currentTimeMillis())
        .nextInt();
  }
/*
得到UserParagraphMap字段内容，对外调用这个私有化字段用的
 */
  public Map<String, Paragraph> getUserParagraphMap() {
    return userParagraphMap;
  }
/*
根据user得到其对应的paragraph对象，里面包含了一系列该对象的属性内容，调用cloneParagraphForUser方法
 */
  public Paragraph getUserParagraph(String user) {
    if (!userParagraphMap.containsKey(user)) {
      cloneParagraphForUser(user);
    }
    return userParagraphMap.get(user);
  }

  @Override
  public void setResult(Object results) {
    this.results = results;//设置最终的返回结果
  }

  public Paragraph cloneParagraphForUser(String user) {
    Paragraph p = new Paragraph();
    p.settings.setParams(Maps.newHashMap(settings.getParams()));
    p.settings.setForms(Maps.newLinkedHashMap(settings.getForms()));
    p.setConfig(Maps.newHashMap(config));
    p.setTitle(getTitle());
    p.setText(getText());
    p.setResult(getReturn());
    p.setStatus(Status.READY);
    p.setId(getId());
    addUser(p, user);
    return p;
  }
/*
清除这个对象里的内容
 */
  public void clearUserParagraphs() {
    userParagraphMap.clear();
  }

  public void addUser(Paragraph p, String user) {
    userParagraphMap.put(user, p);
  }

  public String getUser() {
    return user;
  }
/*
得到
 paragraph对象的text输入内容*/
  public String getText() {
    return text;
  }

  public void setText(String newText) {
    this.text = newText;
    this.dateUpdated = new Date();
  }

  public AuthenticationInfo getAuthenticationInfo() {
    return authenticationInfo;
  }

  public void setAuthenticationInfo(AuthenticationInfo authenticationInfo) {
    this.authenticationInfo = authenticationInfo;
    this.user = authenticationInfo.getUser();
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
/*
* 设置note参数*/
  public void setNote(Note note) {
    this.note = note;
  }

  public Note getNote() {
    return note;
  }

  public boolean isEnabled() {
    Boolean enabled = (Boolean) config.get("enabled");
    return enabled == null || enabled.booleanValue();
  }

  public String getRequiredReplName() {
    return getRequiredReplName(text);
  }
/*
得到用户选择的解析器名字比如%sql
 */
  public static String getRequiredReplName(String text) {
    if (text == null) {
      return null;
    }

    String trimmed = text.trim();
    if (!trimmed.startsWith("%")) {
      return null;
    }

    // get script head
    /*
    得到段落开头即选择的解析器，比如%sql
     */
    int scriptHeadIndex = 0;
    for (int i = 0; i < trimmed.length(); i++) {
      char ch = trimmed.charAt(i);
      /*
      从text开头遍历，遇到回车或者（ 或者空白字符就停止，然后截取text字段
       */
      if (Character.isWhitespace(ch) || ch == '(' || ch == '\n') {
        break;
      }
      scriptHeadIndex = i;
    }
    if (scriptHeadIndex < 1) {
      return null;
    }
    String head = text.substring(1, scriptHeadIndex + 1);
    return head;
  }
/*
得到用户具体输入的字段内容，比如select * from test1
从获得解析器的下标开始截取到最后 就是用户的代码
 */
  public String getScriptBody() {
    return getScriptBody(text);
  }

  public static String getScriptBody(String text) {
    if (text == null) {
      return null;
    }

    String magic = getRequiredReplName(text);
    if (magic == null) {
      return text;
    }

    String trimmed = text.trim();
    if (magic.length() + 1 >= trimmed.length()) {
      return "";
    }
    return trimmed.substring(magic.length() + 1).trim();
  }

  public Interpreter getRepl(String name) {
    /*
    通过interpreterBindings，获取Map<String, List<String>> interpreterBindings;
      key：noteId，value：Interpreter的settings<list>
     */
    return factory.getInterpreter(user, note.getId(), name);
  }
/*
通过获取的用户代码段引用的解析器名字从factory里面拿出对应的解析器
 */
  public Interpreter getCurrentRepl() {
    return getRepl(getRequiredReplName());
  }

  public List<InterpreterCompletion> getInterpreterCompletion() {
    List<InterpreterCompletion> completion = new LinkedList();

    /*
     根据noteId得到对应的解释器setting，将其进行遍历存入到具体对象InterpreterInf list集合中
 */
    for (InterpreterSetting intp : interpreterSettingManager.getInterpreterSettings(note.getId())) {
      /*

    infos<String，String>：
    还有相关interpreterGroup信息，dependencies，interpreterfactory等等与interpreter相关信息
     */
      /*
      intInfo是 InterpreterSetting类的   interpreterInfos字段 ，这个字段是一个List集合，存的InterpreterSetting对象；
     InterpreterSetting里 有着Interpreter相关信息；properties：属性
    status：状态；errorReason：错误原因等字段可以获取
       */
      List<InterpreterInfo> intInfo = intp.getInterpreterInfos();
      if (intInfo.size() > 1) {
        for (InterpreterInfo info : intInfo) {
          /*
          intp.getName() + "." + info.getName();
          解释器的名字.信息属性的名字来表示这个唯一确定的对象
           */
          String name = intp.getName() + "." + info.getName();
          completion.add(new InterpreterCompletion(name, name));
        }
      } else {
        completion.add(new InterpreterCompletion(intp.getName(), intp.getName()));
      }
    }
    return completion;
  }
/*
对写的代码的具体解析
 */
  public List<InterpreterCompletion> completion(String buffer, int cursor) {
    String lines[] = buffer.split(System.getProperty("line.separator"));
    if (lines.length > 0 && lines[0].startsWith("%") && cursor <= lines[0].trim().length()) {

      int idx = lines[0].indexOf(' ');
      if (idx < 0 || (idx > 0 && cursor <= idx)) {
        return getInterpreterCompletion();
      }
    }

    String replName = getRequiredReplName(buffer);
    if (replName != null && cursor > replName.length()) {
      cursor -= replName.length() + 1;
    }

    String body = getScriptBody(buffer);
    Interpreter repl = getRepl(replName);
    if (repl == null) {
      return null;
    }

    List completion = repl.completion(body, cursor);
    return completion;
  }

  public void setInterpreterFactory(InterpreterFactory factory) {
    this.factory = factory;
  }

  public void setInterpreterSettingManager(InterpreterSettingManager interpreterSettingManager) {
    this.interpreterSettingManager = interpreterSettingManager;
  }
/*
得到最终结果
 */
  public InterpreterResult getResult() {
    return (InterpreterResult) getReturn();
  }

  @Override
  public Object getReturn() {
    return results;
  }

  public Object getPreviousResultFormat() {
    return result;
  }

  @Override
  /*
  执行，
   */
  public int progress() {
    String replName = getRequiredReplName();//得到%sql
    Interpreter repl = getRepl(replName);//得到具体解析器
    if (repl != null) {
      return repl.getProgress(getInterpreterContext(null));
    } else {
      return 0;
    }
  }

  @Override
  public Map<String, Object> info() {
    return null;
  }

  private boolean hasPermission(String user, List<String> intpUsers) {
    if (1 > intpUsers.size()) {
      return true;
    }

    for (String u : intpUsers) {
      if (user.trim().equals(u.trim())) {
        return true;
      }
    }
    return false;
  }

  public boolean isBlankParagraph() {
    return Strings.isNullOrEmpty(getText()) || getText().trim().equals(getMagic());
  }


  @Override
  protected Object jobRun() throws Throwable {
    String replName = getRequiredReplName();
    Interpreter repl = getRepl(replName);
    logger.info("run paragraph {} using {} " + repl, getId(), replName);
    if (repl == null) {
      logger.error("Can not find interpreter name " + repl);
      throw new RuntimeException("Can not find interpreter for " + getRequiredReplName());
    }
    InterpreterSetting intp = getInterpreterSettingById(repl.getInterpreterGroup().getId());
    while (intp.getStatus().equals(
        org.apache.zeppelin.interpreter.InterpreterSetting.Status.DOWNLOADING_DEPENDENCIES)) {
      Thread.sleep(200);
    }
    if (this.noteHasUser() && this.noteHasInterpreters()) {
      if (intp != null && interpreterHasUser(intp)
          && isUserAuthorizedToAccessInterpreter(intp.getOption()) == false) {
        logger.error("{} has no permission for {} ", authenticationInfo.getUser(), repl);
        return new InterpreterResult(Code.ERROR,
            authenticationInfo.getUser() + " has no permission for " + getRequiredReplName());
      }
    }

    for (Paragraph p : userParagraphMap.values()) {
      p.setText(getText());
    }

    String script = getScriptBody();
    // inject form
    if (repl.getFormType() == FormType.NATIVE) {
      settings.clear();
    } else if (repl.getFormType() == FormType.SIMPLE) {
      String scriptBody = getScriptBody();
      // inputs will be built from script body
      LinkedHashMap<String, Input> inputs = Input.extractSimpleQueryForm(scriptBody);

      final AngularObjectRegistry angularRegistry =
          repl.getInterpreterGroup().getAngularObjectRegistry();

      scriptBody = extractVariablesFromAngularRegistry(scriptBody, inputs, angularRegistry);

      settings.setForms(inputs);
      script = Input.getSimpleQuery(settings.getParams(), scriptBody);
    }
    logger.debug("RUN : " + script);
    try {
      InterpreterContext context = getInterpreterContext();
      InterpreterContext.set(context);
      InterpreterResult ret = repl.interpret(script, context);

      if (Code.KEEP_PREVIOUS_RESULT == ret.code()) {
        return getReturn();
      }

      context.out.flush();
      List<InterpreterResultMessage> resultMessages = context.out.toInterpreterResultMessage();
      resultMessages.addAll(ret.message());

      InterpreterResult res = new InterpreterResult(ret.code(), resultMessages);

      Paragraph p = getUserParagraph(getUser());
      if (null != p) {
        p.setResult(res);
        p.settings.setParams(settings.getParams());
      }
      /**将用户信息发送到log中，形式为日志格式:
       *  {time}: {user_name}: {sql}
       *  */
      logger.info("{" + dateUpdated + "}:" +
              "{" + authenticationInfo.getUser() + "}:" +
              "{" + "{" + userParagraphMap.get(authenticationInfo.getUser()).text + "}" );
      return res;
    } finally {
      /**将用户信息发送到log中，形式为日志格式:
       *  {time}: {user_name}: {sql}
       *  */
      logger.info("{" + dateUpdated + "}:",
              "{" + authenticationInfo.getUser() + "}:",
              "{" + "{" + text + "}" + "\t");
      InterpreterContext.remove();
    }
  }

  private boolean noteHasUser() {
    return this.user != null;
  }

  private boolean noteHasInterpreters() {
    return !interpreterSettingManager.getInterpreterSettings(note.getId()).isEmpty();
  }

  private boolean interpreterHasUser(InterpreterSetting intp) {
    return intp.getOption().permissionIsSet() && intp.getOption().getUsers() != null;
  }

  private boolean isUserAuthorizedToAccessInterpreter(InterpreterOption intpOpt) {
    return intpOpt.permissionIsSet() && hasPermission(authenticationInfo.getUser(),
        intpOpt.getUsers());
  }

  private InterpreterSetting getInterpreterSettingById(String id) {
    InterpreterSetting setting = null;
    for (InterpreterSetting i : interpreterSettingManager.getInterpreterSettings(note.getId())) {
      if (id.startsWith(i.getId())) {
        setting = i;
        break;
      }
    }
    return setting;
  }

  @Override
  protected boolean jobAbort() { //中止job
    Interpreter repl = getRepl(getRequiredReplName());
    if (repl == null) {
      // when interpreters are already destroyed
      return true;
    }

    Scheduler scheduler = repl.getScheduler();
    if (scheduler == null) {
      return true;
    }

    Job job = scheduler.removeFromWaitingQueue(getId());
    if (job != null) {
      job.setStatus(Status.ABORT);
    } else {
      repl.cancel(getInterpreterContextWithoutRunner(null));
    }
    return true;
  }

  private InterpreterContext getInterpreterContext() {
    final Paragraph self = this;

    return getInterpreterContext(new InterpreterOutput(new InterpreterOutputListener() {
      @Override
      public void onAppend(int index, InterpreterResultMessageOutput out, byte[] line) {
        ((ParagraphJobListener) getListener()).onOutputAppend(self, index, new String(line));
      }

      @Override
      public void onUpdate(int index, InterpreterResultMessageOutput out) {
        try {
          ((ParagraphJobListener) getListener())
              .onOutputUpdate(self, index, out.toInterpreterResultMessage());
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }

      @Override
      public void onUpdateAll(InterpreterOutput out) {
        try {
          List<InterpreterResultMessage> messages = out.toInterpreterResultMessage();
          ((ParagraphJobListener) getListener()).onOutputUpdateAll(self, messages);
          updateParagraphResult(messages);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }

      }

      private void updateParagraphResult(List<InterpreterResultMessage> msgs) {
        // update paragraph result
        InterpreterResult result = new InterpreterResult(Code.SUCCESS, msgs);
        setReturn(result, null);
      }
    }));
  }

  private InterpreterContext getInterpreterContextWithoutRunner(InterpreterOutput output) {
    AngularObjectRegistry registry = null;
    ResourcePool resourcePool = null;

    if (!interpreterSettingManager.getInterpreterSettings(note.getId()).isEmpty()) {
      InterpreterSetting intpGroup =
          interpreterSettingManager.getInterpreterSettings(note.getId()).get(0);
      registry = intpGroup.getInterpreterGroup(getUser(), note.getId()).getAngularObjectRegistry();
      resourcePool = intpGroup.getInterpreterGroup(getUser(), note.getId()).getResourcePool();
    }

    List<InterpreterContextRunner> runners = new LinkedList<>();

    final Paragraph self = this;

    Credentials credentials = note.getCredentials();
    if (authenticationInfo != null) {
      UserCredentials userCredentials =
          credentials.getUserCredentials(authenticationInfo.getUser());
      authenticationInfo.setUserCredentials(userCredentials);
    }

    InterpreterContext interpreterContext =
        new InterpreterContext(note.getId(), getId(), getRequiredReplName(), this.getTitle(),
            this.getText(), this.getAuthenticationInfo(), this.getConfig(), this.settings, registry,
            resourcePool, runners, output);
    return interpreterContext;
  }

  private InterpreterContext getInterpreterContext(InterpreterOutput output) {
    AngularObjectRegistry registry = null;
    ResourcePool resourcePool = null;

    if (!interpreterSettingManager.getInterpreterSettings(note.getId()).isEmpty()) {
      InterpreterSetting intpGroup =
          interpreterSettingManager.getInterpreterSettings(note.getId()).get(0);
      registry = intpGroup.getInterpreterGroup(getUser(), note.getId()).getAngularObjectRegistry();
      resourcePool = intpGroup.getInterpreterGroup(getUser(), note.getId()).getResourcePool();
    }

    List<InterpreterContextRunner> runners = new LinkedList<>();
    for (Paragraph p : note.getParagraphs()) {
      runners.add(new ParagraphRunner(note, note.getId(), p.getId()));
    }

    final Paragraph self = this;
//身份验证
    Credentials credentials = note.getCredentials();
    if (authenticationInfo != null) {
      UserCredentials userCredentials =
          credentials.getUserCredentials(authenticationInfo.getUser());
      authenticationInfo.setUserCredentials(userCredentials);
    }
/*
将interpreter相关信息放到context中，比如noteid，interpreterId,interpretermingzi，。。。反正相关信息都扔里面了
 */
    InterpreterContext interpreterContext =
        new InterpreterContext(note.getId(), getId(), getRequiredReplName(), this.getTitle(),
            this.getText(), this.getAuthenticationInfo(), this.getConfig(), this.settings, registry,
            resourcePool, runners, output);
    return interpreterContext;
  }

  public InterpreterContextRunner getInterpreterContextRunner() {
/*
ParagraphRunner（note对象，noteid，jobid）
note里面是对paragraph的一系列解析呀管理呀设置呀包括对应的Interpreter的信息呀，监听等，并且控制paragraph的crut
 */
    return new ParagraphRunner(note, note.getId(), getId());
  }

  public void setStatusToUserParagraph(Status status) {
    String user = getUser();
    if (null != user) {
      getUserParagraph(getUser()).setStatus(status);
    }
  }
/*
代码真正执行的的地方，调用了note类中的run方法
 */
  static class ParagraphRunner extends InterpreterContextRunner {

    private transient Note note;

    public ParagraphRunner(Note note, String noteId, String paragraphId) {
      super(noteId, paragraphId);
      this.note = note;
    }

    @Override
    public void run() {
      note.run(getParagraphId());
    }
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public void setReturn(InterpreterResult value, Throwable t) {
    setResult(value);
    setException(t);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    Paragraph paraClone = (Paragraph) this.clone();
    return paraClone;
  }

  private String getApplicationId(HeliumPackage pkg) {
    return "app_" + getNote().getId() + "-" + getId() + pkg.getName().replaceAll("\\.", "_");
  }

  public ApplicationState createOrGetApplicationState(HeliumPackage pkg) {
    synchronized (apps) {
      for (ApplicationState as : apps) {
        if (as.equals(pkg)) {
          return as;
        }
      }

      String appId = getApplicationId(pkg);
      ApplicationState appState = new ApplicationState(appId, pkg);
      apps.add(appState);
      return appState;
    }
  }


  public ApplicationState getApplicationState(String appId) {
    synchronized (apps) {
      for (ApplicationState as : apps) {
        if (as.getId().equals(appId)) {
          return as;
        }
      }
    }

    return null;
  }

  public List<ApplicationState> getAllApplicationStates() {
    synchronized (apps) {
      return new LinkedList<>(apps);
    }
  }

  String extractVariablesFromAngularRegistry(String scriptBody, Map<String, Input> inputs,
      AngularObjectRegistry angularRegistry) {

    final String noteId = this.getNote().getId();
    final String paragraphId = this.getId();

    final Set<String> keys = new HashSet<>(inputs.keySet());

    for (String varName : keys) {
      final AngularObject paragraphScoped = angularRegistry.get(varName, noteId, paragraphId);
      final AngularObject noteScoped = angularRegistry.get(varName, noteId, null);
      final AngularObject angularObject = paragraphScoped != null ? paragraphScoped : noteScoped;
      if (angularObject != null) {
        inputs.remove(varName);
        final String pattern = "[$][{]\\s*" + varName + "\\s*(?:=[^}]+)?[}]";
        scriptBody = scriptBody.replaceAll(pattern, angularObject.get().toString());
      }
    }
    return scriptBody;
  }

  public String getMagic() {
    String magic = StringUtils.EMPTY;
    String text = getText();
    if (text != null && text.startsWith("%")) {
      magic = text.split("\\s+")[0];
      if (isValidInterpreter(magic.substring(1))) {
        return magic;
      } else {
        return StringUtils.EMPTY;
      }
    }
    return magic;
  }

  private boolean isValidInterpreter(String replName) {
    try {
      return factory.getInterpreter(user, note.getId(), replName) != null;
    } catch (InterpreterException e) {
      // ignore this exception, it would be recaught when running paragraph.
      return false;
    }
  }
}
