// A draft is kept in memory until the complete movement passes validation.
window.NekoManual=(()=>{
 const $=id=>document.getElementById(id),t=(key,vars)=>NekoI18n.t(key,vars);
 let parts=[],split=false,categoryChosen=false,saving=false;
 const cents=value=>Math.round(Number(value)*100);
 function validateParts(rows,total,categories){
   if(rows.length<2||rows.some(p=>!categories.includes(p.category)||!Number.isFinite(Number(p.amount))||Number(p.amount)<=0||Math.abs(Number(p.amount)*100-cents(p.amount))>.000001))return "manual_invalid_parts";
   if(new Set(rows.map(p=>p.category)).size!==rows.length)return "manual_distinct_categories";
   if(rows.reduce((sum,p)=>sum+cents(p.amount),0)!==total)return "manual_split_mismatch";
   return null;
 }
 function updateBalance(){
   const target=cents($("mamount").value)||0,total=parts.reduce((sum,p)=>sum+(cents(p.amount)||0),0),difference=target-total;
   $("manualSplitBalance").textContent=t(difference===0?"manual_total_ok":difference>0?"manual_remaining":"manual_excess",{amount:euro(Math.abs(difference)/100)});
   $("manualSplitBalance").className=difference===0?"status ok":"status err";
 }
 function categoryButton(button,category){
   button.innerHTML=`<b>${esc(category)}</b><span aria-hidden="true">›</span>`;
 }
 function chooseCategory(current,onChoose){
   if(document.activeElement instanceof HTMLElement)document.activeElement.blur();
   const root=$("manualCategoryOptions");
   root.innerHTML=CATS().map(category=>`<button type="button" class="previewCategoryOption ${category===current?"active":""}" data-cat="${esc(category)}"><span>${esc(category)}</span><span class="previewCategoryCheck">✓</span></button>`).join("");
   root.querySelectorAll("button").forEach(button=>button.onclick=()=>{onChoose(button.dataset.cat);$("manualCategoryModal").style.display="none"});
   $("manualCategoryModal").style.display="flex";
 }
 function renderParts(){
   const root=$("manualSplitRows");root.innerHTML="";
   parts.forEach((part,index)=>{
     const row=document.createElement("div");row.className="manualSplitRow";
     const category=document.createElement("button");category.type="button";category.className="manualCategoryButton";categoryButton(category,part.category);
     category.onclick=()=>chooseCategory(part.category,value=>{part.category=value;categoryButton(category,value);updateBalance()});
     const amount=document.createElement("input");amount.type="number";amount.min="0.01";amount.step="0.01";amount.inputMode="decimal";amount.value=part.amount||"";amount.placeholder="0,00";amount.setAttribute("aria-label",t("manual_part_amount",{number:index+1}));
     amount.oninput=()=>{part.amount=Number(amount.value);updateBalance()};
     const remove=document.createElement("button");remove.type="button";remove.className="danger";remove.textContent="×";remove.disabled=parts.length<=2;remove.setAttribute("aria-label",t("manual_remove_part"));remove.onclick=()=>{parts.splice(index,1);renderParts()};
     row.append(category,amount,remove);root.append(row);
   });updateBalance();
 }
 function updateView(){
   const expense=$("mtype").value==="expense",enabled=expense&&split;
   $("toggleManualSplit").hidden=!expense;$("toggleManualSplit").setAttribute("aria-expanded",String(enabled));
   $("toggleManualSplit").dataset.i18n=enabled?"manual_remove_split":"manual_split";
   $("toggleManualSplit").textContent=t(enabled?"manual_remove_split":"manual_split");
   $("manualSplitPanel").hidden=!enabled;$("manualCategoryButton").hidden=enabled;$("manualCategoryLabel").hidden=enabled;
   $("manualTypeName").textContent=t(expense?"expenses":"income");
   $("manualCategoryName").textContent=$("mcat").value;
   // Hidden draft fields must not block an income or an unsplit movement.
   $("manualSplitRows").querySelectorAll("input").forEach(input=>input.disabled=!enabled);
 }
 function open(){
   $("manualForm").reset();parts=[];split=false;categoryChosen=false;saving=false;
   const date=new Date();$("mdate").value=`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`;
   $("mcat").value="Da classificare";$("mtype").value="expense";$("manualError").textContent="";$("manualSplitRows").innerHTML="";
   updateView();NekoI18n.apply();$("manualModal").style.display="flex";document.body.classList.add("manual-modal-open");
 }
 function close(){
   if(document.activeElement instanceof HTMLElement)document.activeElement.blur();
   $("manualTypeModal").style.display="none";$("manualCategoryModal").style.display="none";$("manualModal").style.display="none";document.body.classList.remove("manual-modal-open");
 }
 function bind(){
   $("closeManual").onclick=close;$("closeManualCategory").onclick=()=>$("manualCategoryModal").style.display="none";
   $("manualCategoryButton").onclick=()=>chooseCategory($("mcat").value,value=>{$("mcat").value=value;categoryChosen=value!=="Da classificare";updateView()});
   $("closeManualType").onclick=()=>$("manualTypeModal").style.display="none";
   $("manualTypeButton").onclick=()=>{
     if(document.activeElement instanceof HTMLElement)document.activeElement.blur();
     const root=$("manualTypeOptions");
     root.innerHTML=["expense","income"].map(value=>'<button type="button" class="previewCategoryOption '+(value===$("mtype").value?'active':'')+'" data-type="'+value+'" aria-pressed="'+(value===$("mtype").value)+'"><span>'+t(value==="expense"?"expenses":"income")+'</span><span class="previewCategoryCheck" aria-hidden="true">✓</span></button>').join("");
     root.querySelectorAll("button").forEach(button=>button.onclick=()=>{$("mtype").value=button.dataset.type;$("manualError").textContent="";updateView();$("manualTypeModal").style.display="none"});
     $("manualTypeModal").style.display="flex";
   };
   $("mamount").addEventListener("input",updateBalance);
   $("toggleManualSplit").onclick=()=>{
     split=!split;
     if(split&&!parts.length){const total=cents($("mamount").value)||0,first=Math.floor(total/2),category=$("mcat").value;parts=[{category,amount:first/100},{category:CATS().find(c=>c!==category)||category,amount:(total-first)/100}]}
     if(split)renderParts();updateView();
   };
   $("addManualSplit").onclick=()=>{parts.push({category:CATS().find(c=>!parts.some(p=>p.category===c))||CATS()[0],amount:0});renderParts()};
   $("manualForm").onsubmit=event=>{
     event.preventDefault();if(saving)return;$("manualError").textContent="";
     const raw=Number($("mamount").value),total=cents(raw),description=sanitizeDescription($("mdesc").value.trim()),date=$("mdate").value;
     if(!Number.isFinite(raw)||total<=0||Math.abs(raw*100-total)>.000001||!description||!/^\d{4}-\d{2}-\d{2}$/.test(date)){$("manualError").textContent=t("manual_invalid");return}
     const expense=$("mtype").value==="expense",hasSplit=expense&&split;
     const error=hasSplit?validateParts(parts,total,CATS()):null;if(error){$("manualError").textContent=t(error);return}
     const amount=(expense?-total:total)/100,classification=classify(description,amount),category=categoryChosen?$("mcat").value:classification.cat;
     const movement={id:uid(),date:itDate(date),valueDate:itDate(date),amount,description,category,why:"manual",source:"manual",note:"",excludeStats:false};
     if(hasSplit){movement.splitParts=parts.map(p=>({category:p.category,amount:cents(p.amount)/100}));movement.category=movement.splitParts[0].category}
     saving=true;moves.push(movement);
     try{localStorage.setItem("micio_v5_moves",JSON.stringify(moves))}catch(error){moves=moves.filter(m=>m.id!==movement.id);saving=false;$("manualError").textContent=t("manual_save_error");return}
     close();render();go("moves");
   };
 }
 return {open,close,bind,validateParts};
})();
