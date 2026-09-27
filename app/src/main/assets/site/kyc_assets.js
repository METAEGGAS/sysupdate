var kycHTML='<div class="kyin"><div class="wdt"><div class="pfb" onclick="closeKyc()"><img src="https://i.ibb.co/k69JQ9LS/IMG.png" alt="back"></div><h1 data-t="kyc">التحقق من الهوية</h1><div class="dsp"></div></div><div id="kycForm"><div class="ksub" data-t="kycSub">يرجى تقديم بيانات حقيقية، حيث أن البيانات المزيفة لن تحظى بحماية من أي نوع</div><div class="fld"><input id="kycName" data-ph="phName" placeholder="الاسم الحقيقي"></div><div class="fld"><input id="kycIdNum" inputmode="numeric" data-ph="phId" placeholder="رقم الهوية"></div><div class="fld" onclick="selDoc()"><span class="ph" id="docPh"></span><svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg></div><div class="ksec" data-t="idPic">صورة الهوية</div><div class="ids"><div class="idb" onclick="pick(\'f\')"><div class="idf"><i class="tl"></i><i class="tr"></i><i class="bl"></i><i class="br"></i><img id="imf" src="https://i.ibb.co/1DK8KR8/IMG.png" alt="front"></div><p data-t="idFront">صورة امامية الهوية</p><input type="file" id="pf" accept="image/*" onchange="setImg(\'f\',this)"></div><div class="idb" onclick="pick(\'b\')"><div class="idf"><i class="tl"></i><i class="tr"></i><i class="bl"></i><i class="br"></i><img id="imb" src="https://i.ibb.co/JWV5ZCcm/IMG.png" alt="back"></div><p data-t="idBack">صورة خلفية الهوية</p><input type="file" id="pb" accept="image/*" onchange="setImg(\'b\',this)"></div></div><button class="kgo" id="kycGoBtn" onclick="subKyc()" data-t="submit">إرسال التعديلات</button></div><div id="kycStatus" style="display:none;text-align:center;padding:70px 24px 40px"><img id="kycStImg" src="https://i.ibb.co/JWYFWVPG/IMG.png" alt="status" style="width:130px;height:130px;object-fit:contain;display:block;margin:0 auto"><div id="kycStTxt" style="margin-top:20px;font-size:19px;font-weight:700;color:#e8eef8">جاري المراجعة والتدقيق</div><div id="kycStSub" style="margin-top:10px;font-size:13px;color:#8fa3c0;line-height:1.8"></div></div></div>';

// ============================================================
// ===== Firebase: جلسة المستخدم + حفظ/قراءة بيانات التحقق =====
// ===== نفس طريقة ملف التداول (trd_assets.js) بالضبط =====
// ============================================================
var KYC_FB_CFG={apiKey:"AIzaSyBvzfJOOjRFZnTgTUrwEZQPr8Ba7zKKlNg",authDomain:"hhhxh-5ebe4.firebaseapp.com",projectId:"hhhxh-5ebe4",storageBucket:"hhhxh-5ebe4.firebasestorage.app",messagingSenderId:"79243000696",appId:"1:79243000696:web:ee0fb2d2ccce791954e68d",measurementId:"G-08BR6LN6PT"};
var kycFB={ready:false,uid:null,db:null,auth:null,fs:null,busy:false};
var kycImgs={f:null,b:null};var kycLastSt=null; // صور الهوية بعد الضغط (base64)

// ===== تحميل Firebase والتحقق من جلسة المستخدم في الخلفية =====
(async function(){
  try{
    var authM=await import('https://www.gstatic.com/firebasejs/12.19.0/firebase-auth.js');
    var fsM=await import('https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js');
    kycFB.fs=fsM;
    // الربط بجلسة المستخدم المسجّل في الصفحة الرئيسية (بدون إنشاء تطبيق ثانٍ أو دخول مجهول)
    var app=(window.__exFB&&window.__exFB.app)?window.__exFB.app:null;
    if(app){
      kycFB.auth=window.__exFB.auth||authM.getAuth(app);
      kycFB.db=window.__exFB.db||fsM.getFirestore(app);
    }else{
      var appM=await import('https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js');
      var app2=appM.initializeApp(KYC_FB_CFG,'kycApp');
      kycFB.auth=authM.getAuth(app2);
      kycFB.db=fsM.getFirestore(app2);
    }
    // التحقق من جلسة المستخدم بالخلفية — نفس جلسة المستخدم المسجّل
    authM.onAuthStateChanged(kycFB.auth,async function(u){
      try{
        if(!u){kycFB.ready=false;kycFB.uid=null;return;}
        kycFB.uid=u.uid;
        kycFB.ready=true;
        await kycEnsureUserDoc();
        await kycLoadStatus();
      }catch(e){}
    });
  }catch(e){}
})();

// ===== إنشاء مستند المستخدم إن لم يكن موجوداً (نفس مجموعة users المستخدمة بملف التداول) =====
async function kycEnsureUserDoc(){
  var fs=kycFB.fs,ref=fs.doc(kycFB.db,'users',kycFB.uid);
  var snap=await fs.getDoc(ref);
  if(!snap.exists()){await fs.setDoc(ref,{balance:0,uses:[],createdAt:Date.now()},{merge:true});}
}

// ===== قراءة حالة التحقق من Firebase وعرضها عند فتح الصفحة =====
async function kycLoadStatus(){
  var fs=kycFB.fs,ref=fs.doc(kycFB.db,'kyc',kycFB.uid);
  var snap=await fs.getDoc(ref);
  if(!snap.exists())return; // لم يقدّم طلباً من قبل — يبقى النموذج ظاهراً
  kycShowStatus(snap.data());
}

// ===== عرض شاشة الحالة (قيد المراجعة / مقبول / مرفوض) =====
function kycShowStatus(d){if(d)kycLastSt=d;
  var form=document.getElementById('kycForm'),st=document.getElementById('kycStatus');
  if(!form||!st)return;
  var txt=document.getElementById('kycStTxt'),sub=document.getElementById('kycStSub');
  var status=(d&&d.status)||'pending';
  if(status==='approved'){
    form.style.display='none';st.style.display='block';
    if(txt)txt.textContent=_t('kycApproved');
    if(sub)sub.textContent=_t('kycApprovedSub');
  }else if(status==='rejected'){
    form.style.display='block';st.style.display='none';
    kycShowResult(false,_t('kycRejected'),((d&&d.note)||_t('kycRejectedNote'))+'<br>'+_t('kycRejectedRetry'));
  }else{ // pending
    form.style.display='none';st.style.display='block';
    if(txt)txt.textContent=_t('kycPending');
    if(sub)sub.textContent=_t('kycPendingSub');
  }
}

// ===== طبقة التحميل السوداء الشفافة (نفس نمط ملف التداول) =====
function kycShowLoading(){
  kycHideLoading();
  var ov=document.createElement('div');
  ov.id='kycOverlay';
  ov.style.cssText='position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.72);z-index:99998;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px';
  ov.innerHTML='<div style="width:52px;height:52px;border:4px solid rgba(255,255,255,.15);border-top-color:#2f7bf6;border-radius:50%;animation:trdspin .8s linear infinite"></div>'
  +'<div style="font-size:14px;font-weight:600;color:#cfe2ff;letter-spacing:.3px">'+_t('kycSending')+'</div>';
  document.body.appendChild(ov);
}
function kycHideLoading(){var o=document.getElementById('kycOverlay');if(o)o.remove();}

// ===== رسالة نجاح / خطأ بملء الشاشة (نفس نمط ملف التداول) =====
function kycShowResult(ok,title,msg){
  kycHideLoading();
  var old=document.getElementById('kycResult');if(old)old.remove();
  var color=ok?'#22c55e':'#ef4444';
  var icon=ok
    ?'<svg width="74" height="74" viewBox="0 0 24 24" fill="none" stroke="'+color+'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="8 12.5 11 15.5 16 9.5"/></svg>'
    :'<svg width="74" height="74" viewBox="0 0 24 24" fill="none" stroke="'+color+'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="9" y1="9" x2="15" y2="15"/><line x1="15" y1="9" x2="9" y2="15"/></svg>';
  var d=document.createElement('div');
  d.id='kycResult';
  d.style.cssText='position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(3,7,17,.96);z-index:99999;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:24px';
  d.innerHTML='<div style="margin-bottom:18px;filter:drop-shadow(0 0 18px '+(ok?'rgba(34,197,94,.5)':'rgba(239,68,68,.5)')+')">'+icon+'</div>'
  +'<div style="font-size:24px;font-weight:800;color:'+color+';margin-bottom:10px">'+title+'</div>'
  +'<div style="font-size:15px;font-weight:500;color:#e8f1ff;line-height:1.9;max-width:320px">'+msg+'</div>'
  +'<div onclick="this.parentElement.remove()" style="margin-top:30px;border-radius:10px;background:linear-gradient(135deg,#1a5fb4 0%,#2f7bf6 55%,#00b8e5 100%);border:1px solid rgba(160,215,255,.5);padding:13px 44px;cursor:pointer;box-shadow:0 10px 30px rgba(20,90,220,.45)"><span style="font-size:15px;font-weight:700;color:#fff">'+_t('kycOk')+'</span></div>';
  document.body.appendChild(d);
}

// ===== فتح منتقي الملفات (أمامية/خلفية) =====
function pick(side){
  var inp=document.getElementById(side==='f'?'pf':'pb');
  if(inp)inp.click();
}

// ===== ضغط الصورة وتحويلها إلى base64 ثم عرضها (لتبقى ضمن حد مستند Firestore) =====
function setImg(side,input){
  try{
    var file=input&&input.files&&input.files[0];
    if(!file)return;
    var reader=new FileReader();
    reader.onload=function(ev){
      var img=new Image();
      img.onload=function(){
        try{
          var max=900,w=img.width,h=img.height;
          if(w>max||h>max){var r=Math.min(max/w,max/h);w=Math.round(w*r);h=Math.round(h*r);}
          var cv=document.createElement('canvas');cv.width=w;cv.height=h;
          cv.getContext('2d').drawImage(img,0,0,w,h);
          var data=cv.toDataURL('image/jpeg',0.72);
          kycImgs[side]=data;
          var el=document.getElementById(side==='f'?'imf':'imb');
          if(el)el.src=data;
        }catch(e){
          kycImgs[side]=ev.target.result;
          var el2=document.getElementById(side==='f'?'imf':'imb');
          if(el2)el2.src=ev.target.result;
        }
      };
      img.src=ev.target.result;
    };
    reader.readAsDataURL(file);
  }catch(e){}
}

// ===== معالج زر الإرسال: حفظ الطلب في Firebase (مجموعة kyc بنفس uid المستخدم) =====
async function subKyc(){
  if(kycFB.busy)return;
  var nameEl=document.getElementById('kycName');
  var idEl=document.getElementById('kycIdNum');
  var name=nameEl?nameEl.value.trim():'';
  var idNum=idEl?idEl.value.trim():'';
  var docPh=document.getElementById('docPh');
  var docType=(window.kycDocType||window.selDocVal||(docPh?docPh.textContent.trim():'')||'').trim();
  if(!name){kycShowResult(false,_t('kycErr'),_t('kycErrName'));return;}
  if(!idNum){kycShowResult(false,_t('kycErr'),_t('kycErrId'));return;}
  if(!kycImgs.f||!kycImgs.b){kycShowResult(false,_t('kycErr'),_t('kycErrImgs'));return;}
  if(!kycFB.ready){kycShowResult(false,_t('kycErr'),_t('kycErrSession'));return;}
  kycFB.busy=true;
  kycShowLoading();
  try{
    var fs=kycFB.fs;
    var ref=fs.doc(kycFB.db,'kyc',kycFB.uid);
    await fs.setDoc(ref,{
      uid:kycFB.uid,
      name:name,
      idNumber:idNum,
      docType:docType,
      frontImg:kycImgs.f,
      backImg:kycImgs.b,
      status:'pending',
      note:null,
      submittedAt:Date.now(),
      updatedAt:Date.now()
    },{merge:true});
    kycHideLoading();
    kycShowStatus({status:'pending'});
  }catch(e){
    kycShowResult(false,_t('kycErr'),_t('kycErrSend'));
  }finally{
    kycHideLoading();
    kycFB.busy=false;
  }
}

/* ===== اختيار نوع المستند (نُقل من الصفحة الأم — خاص بصفحة التحقق من الهوية) ===== */
var docT=(typeof L!=='undefined'&&L.ar&&L.ar.docs)?L.ar.docs:['هوية وطنية','جواز سفر','رخصة قيادة'],docI=-1;
function selDoc(){var dt=kycDocs();docI=(docI+1)%dt.length;var p=document.getElementById('docPh');if(p){p.textContent=dt[docI];p.style.color='#e8eef8'}}

function _t(k){try{if(typeof T==='function')return T(k)}catch(e){}return (typeof KYC_AR!=='undefined'&&KYC_AR[k])||k}
var KYC_AR={kycApproved:'تم التحقق من هويتك بنجاح',kycApprovedSub:'حسابك موثّق ومحمي بالكامل',kycRejected:'تم رفض الطلب',kycRejectedNote:'البيانات المقدمة غير مطابقة',kycRejectedRetry:'يرجى إعادة التقديم ببيانات صحيحة',kycPending:'جاري المراجعة والتدقيق',kycPendingSub:'سيتم إشعارك فور الانتهاء من مراجعة بياناتك',kycSending:'جاري إرسال البيانات...',kycOk:'حسناً',kycErr:'خطأ',kycErrName:'يرجى إدخال الاسم الحقيقي',kycErrId:'يرجى إدخال رقم الهوية',kycErrImgs:'يرجى إرفاق صورة الهوية الأمامية والخلفية',kycErrSession:'جاري التحقق من الجلسة، حاول مرة أخرى بعد ثوانٍ',kycErrSend:'حدث خطأ أثناء الإرسال، حاول مرة أخرى',docNat:'هوية وطنية',docPass:'جواز سفر',docDrive:'رخصة قيادة'};
function kycDocs(){return [_t('docNat'),_t('docPass'),_t('docDrive')]}
function kycApplyLang(){try{if(typeof docI!=='undefined'&&docI>=0){var dp=document.getElementById('docPh');if(dp)dp.textContent=kycDocs()[docI]}}catch(e){}
try{var st=document.getElementById('kycStatus');if(st&&st.style.display==='block'&&kycLastSt){var s2=(kycLastSt&&kycLastSt.status)||'pending';if(s2!=='rejected')kycShowStatus(kycLastSt)}}catch(e){}}
