/*
* CanvasXpress 8.1 - JavaScript Canvas Library
*
* Copyright (c) 2009-2015 Isaac Neuhaus
*
* imnphd@gmail.com
*
*
* Redistributions of this source code must retain this copyright
* notice and the following disclaimer.
*
* CanvasXpress is licensed under the terms of the Open Source
* GPL version 3.0 license.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
* Commercial use may be granted to the extent that this source code
* does NOT become part of any other Open Source or Commercially licensed
* development library or toolkit without explicit permission.
*
* Network graphs were implemented based on the HeyGraph by Tom Martin
* <http://www.heychinaski.com>.
*
* Thanks to Mingyi Liu for his contributions with the Ext-JS panel and
* network graphs and Charles Tilford for his input to the Genome Browser.
*
*/

/*!
 * sprintf() for JavaScript v.0.4
 *
 * Copyright (c) 2007 Alexandru Marasteanu <http://alexei.417.ro/>
 * Thanks to David Baird (unit test and patch).
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
;function str_repeat(b,a){for(var c=[];a>0;c[--a]=b){}return(c.join(""))}function sprintf(){var g=0,e,h=arguments[g++],k=[],d,j,l,b;while(h){if(d=/^[^\x25]+/.exec(h)){k.push(d[0])}else{if(d=/^\x25{2}/.exec(h)){k.push("%")}else{if(d=/^\x25(?:(\d+)\$)?(\+)?(0|'[^$])?(-)?(\d+)?(?:\.(\d+))?([b-fosuxX])/.exec(h)){if(((e=arguments[d[1]||g++])==null)||(e==undefined)){throw ("Too few arguments.")}if(/[^s]/.test(d[7])&&(typeof(e)!="number")){throw ("Expecting number but found "+typeof(e))}switch(d[7]){case"b":e=e.toString(2);break;case"c":e=String.fromCharCode(e);break;case"d":e=parseInt(e);break;case"e":e=d[6]?e.toExponential(d[6]):e.toExponential();break;case"f":e=d[6]?parseFloat(e).toFixed(d[6]):parseFloat(e);break;case"o":e=e.toString(8);break;case"s":e=((e=String(e))&&d[6]?e.substring(0,d[6]):e);break;case"u":e=Math.abs(e);break;case"x":e=e.toString(16);break;case"X":e=e.toString(16).toUpperCase();break}e=(/[def]/.test(d[7])&&d[2]&&e>0?"+"+e:e);l=d[3]?d[3]=="0"?"0":d[3].charAt(1):" ";b=d[5]-String(e).length;j=d[5]?str_repeat(l,b):"";k.push(d[4]?e+j:j+e)}else{throw ("Huh ?!")}}}h=h.substring(d[0].length)}return k.join("")};/*!
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */
;var dateFormat=function(){var a=/d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,b=/\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,d=/[^-+\dA-Z]/g,c=function(f,e){f=String(f);e=e||2;while(f.length<e){f="0"+f}return f};return function(i,v,q){var g=dateFormat;if(arguments.length==1&&Object.prototype.toString.call(i)=="[object String]"&&!/\d/.test(i)){v=i;i=undefined}i=i?new Date(i):new Date;if(isNaN(i)){throw SyntaxError("invalid date")}v=String(g.masks[v]||v||g.masks["default"]);if(v.slice(0,4)=="UTC:"){v=v.slice(4);q=true}var t=q?"getUTC":"get",l=i[t+"Date"](),e=i[t+"Day"](),j=i[t+"Month"](),p=i[t+"FullYear"](),r=i[t+"Hours"](),k=i[t+"Minutes"](),u=i[t+"Seconds"](),n=i[t+"Milliseconds"](),f=q?0:i.getTimezoneOffset(),h={d:l,dd:c(l),ddd:g.i18n.dayNames[e],dddd:g.i18n.dayNames[e+7],m:j+1,mm:c(j+1),mmm:g.i18n.monthNames[j],mmmm:g.i18n.monthNames[j+12],yy:String(p).slice(2),yyyy:p,h:r%12||12,hh:c(r%12||12),H:r,HH:c(r),M:k,MM:c(k),s:u,ss:c(u),l:c(n,3),L:c(n>99?Math.round(n/10):n),t:r<12?"a":"p",tt:r<12?"am":"pm",T:r<12?"A":"P",TT:r<12?"AM":"PM",Z:q?"UTC":(String(i).match(b)||[""]).pop().replace(d,""),o:(f>0?"-":"+")+c(Math.floor(Math.abs(f)/60)*100+Math.abs(f)%60,4),S:["th","st","nd","rd"][l%10>3?0:(l%100-l%10!=10)*l%10]};return v.replace(a,function(m){return m in h?h[m]:m.slice(1,m.length-1)})}}();dateFormat.masks={"default":"ddd mmm dd yyyy HH:MM:ss",shortDate:"m/d/yy",mediumDate:"mmm d, yyyy",longDate:"mmmm d, yyyy",fullDate:"dddd, mmmm d, yyyy",shortTime:"h:MM TT",mediumTime:"h:MM:ss TT",longTime:"h:MM:ss TT Z",isoDate:"yyyy-mm-dd",isoTime:"HH:MM:ss",isoDateTime:"yyyy-mm-dd'T'HH:MM:ss",isoUtcDateTime:"UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"};dateFormat.i18n={dayNames:["Sun","Mon","Tue","Wed","Thu","Fri","Sat","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"],monthNames:["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec","January","February","March","April","May","June","July","August","September","October","November","December"]};Date.prototype.format=function(a,b){return dateFormat(this,a,b)};var m=Math;var mr=m.round;var ms=m.sin;var mc=m.cos;var abs=m.abs;var sqrt=m.sqrt;function createMatrixIdentity(){return[[1,0,0],[0,1,0],[0,0,1]]}function matrixMultiply(d,c){var b=createMatrixIdentity();for(var a=0;a<3;a++){for(var g=0;g<3;g++){var e=0;for(var f=0;f<3;f++){e+=d[a][f]*c[f][g]}b[a][g]=e}}return b}function copyState(b,a){a.fillStyle=b.fillStyle;a.lineCap=b.lineCap;a.lineJoin=b.lineJoin;a.lineWidth=b.lineWidth;a.miterLimit=b.miterLimit;a.shadowBlur=b.shadowBlur;a.shadowColor=b.shadowColor;a.shadowOffsetX=b.shadowOffsetX;a.shadowOffsetY=b.shadowOffsetY;a.strokeStyle=b.strokeStyle;a.globalAlpha=b.globalAlpha}function CanvasWrapper(a){this.m_=createMatrixIdentity();this.mStack_=[];this.aStack_=[];this.canvas=a;this.strokeStyle="#000";this.fillStyle="#000";this.lineWidth=1;this.lineJoin="miter";this.lineCap="butt";this.miterLimit=1;this.globalAlpha=1}var cwPrototype=CanvasWrapper.prototype;cwPrototype.applyContextProperties=function(){this.canvas.strokeStyle=this.strokeStyle;this.canvas.fillStyle=this.fillStyle;this.canvas.lineWidth=this.lineWidth;this.canvas.lineJoin=this.lineJoin;this.canvas.lineCap=this.lineCap;this.canvas.miterLimit=this.miterLimit;this.canvas.globalAlpha=this.globalAlpha;this.canvas.font=this.font};cwPrototype.beginPath=function(){this.canvas.beginPath()};cwPrototype.moveTo=function(b,a){this.canvas.moveTo(b,a);var c=this.getCoords(b,a);this.currentX_=c.x;this.currentY_=c.y};cwPrototype.lineTo=function(b,a){this.applyContextProperties();this.canvas.lineTo(b,a);var c=this.getCoords(b,a);this.currentX_=c.x;this.currentY_=c.y};cwPrototype.bezierCurveTo=function(c,a,f,e,d,b){this.applyContextProperties();this.canvas.bezierCurveTo(c,a,f,e,d,b);var g=this.getCoords(d,b);this.currentX_=g.x;this.currentY_=g.y};cwPrototype.quadraticCurveTo=function(e,c,b,a){this.applyContextProperties();this.canvas.quadraticCurveTo(e,c,b,a);var d=this.getCoords(b,a);this.currentX_=d.x;this.currentY_=d.y};cwPrototype.arc=function(e,d,f,c,b,a){this.applyContextProperties();this.canvas.arc(e,d,f,c,b,a)};cwPrototype.rect=function(c,b,a,e){this.applyContextProperties();this.canvas.rect(c,b,a,e);var d=this.getCoords(c,b);this.currentX_=d.x;this.currentY_=d.y};cwPrototype.strokeRect=function(c,b,a,e){this.applyContextProperties();this.canvas.strokeRect(c,b,a,e);var d=this.getCoords(c,b);this.currentX_=d.x;this.currentY_=d.y};cwPrototype.fillRect=function(c,b,a,e){this.applyContextProperties();this.canvas.fillRect(c,b,a,e);var d=this.getCoords(c,b);this.currentX_=d.x;this.currentY_=d.y};cwPrototype.createLinearGradient=function(b,d,a,c){this.applyContextProperties();return this.canvas.createLinearGradient(b,d,a,c)};cwPrototype.createRadialGradient=function(d,f,c,b,e,a){this.applyContextProperties();return this.canvas.createRadialGradient(d,f,c,b,e,a)};cwPrototype.stroke=function(a){this.applyContextProperties();this.canvas.stroke(a)};cwPrototype.fill=function(){this.applyContextProperties();this.canvas.fill()};cwPrototype.clearRect=function(c,b,a,d){this.canvas.clearRect(c,b,a,d)};cwPrototype.closePath=function(){this.canvas.closePath()};cwPrototype.measureText=function(a){this.applyContextProperties();return this.canvas.measureText(a)};cwPrototype.fillText=function(d,c,b,a){this.applyContextProperties();this.canvas.fillText(d,c,b,a)};cwPrototype.createPattern=function(b,a){this.canvas.createPattern(b,a)};cwPrototype.getCoords=function(c,b){var a=this.m_;return{x:c*a[0][0]+b*a[1][0]+a[2][0],y:c*a[0][1]+b*a[1][1]+a[2][1]}};cwPrototype.save=function(){this.canvas.save();var a={};copyState(this,a);this.aStack_.push(a);this.mStack_.push(this.m_);this.m_=matrixMultiply(createMatrixIdentity(),this.m_)};cwPrototype.restore=function(){this.canvas.restore();copyState(this.aStack_.pop(),this);this.m_=this.mStack_.pop()};function matrixIsFinite(a){for(var c=0;c<3;c++){for(var b=0;b<2;b++){if(!isFinite(a[c][b])||isNaN(a[c][b])){return false}}}return true}function setM(b,a){if(!matrixIsFinite(a)){return}b.m_=a}cwPrototype.setMatrix=function(a){this.setTransform(a[0][0],a[0][1],a[1][0],a[1][1],a[2][0],a[2][1])};cwPrototype.getMatrix=function(){return this.m_};cwPrototype.translate=function(c,b){this.canvas.translate(c,b);var a=[[1,0,0],[0,1,0],[c,b,1]];setM(this,matrixMultiply(a,this.m_))};cwPrototype.rotate=function(b){this.canvas.rotate(b);var e=mc(b);var d=ms(b);var a=[[e,d,0],[-d,e,0],[0,0,1]];setM(this,matrixMultiply(a,this.m_))};cwPrototype.scale=function(c,b){this.canvas.scale(c,b);var a=[[c,0,0],[0,b,0],[0,0,1]];setM(this,matrixMultiply(a,this.m_))};cwPrototype.transform=function(e,d,g,f,b,a){this.canvas.transform(e,d,g,f,b,a);var c=[[e,d,0],[g,f,0],[b,a,1]];setM(this,matrixMultiply(c,this.m_))};cwPrototype.setTransform=function(e,d,g,f,c,b){this.canvas.setTransform(e,d,g,f,c,b);var a=[[e,d,0],[g,f,0],[c,b,1]];setM(this,a)};/*!
 * Copyright (c) 2010, Jason Davies.
 *
 * All rights reserved.  This code is based on Bradley White's Java version,
 * which is in turn based on Nicholas Yue's C++ version, which in turn is based
 * on Paul D. Bourke's original Fortran version.  See below for the respective
 * copyright notices.
 *
 * See http://paulbourke.net/papers/conrec for the original
 * paper by Paul D. Bourke.
 *
 * The vector conversion code is based on http://apptree.net/conrec.htm by
 * Graham Cox.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
;
/*!
 * Copyright (c) 1996-1997 Nicholas Yue
 *
 * This software is copyrighted by Nicholas Yue. This code is based on Paul D.
 * Bourke's CONREC.F routine.
 *
 * The authors hereby grant permission to use, copy, and distribute this
 * software and its documentation for any purpose, provided that existing
 * copyright notices are retained in all copies and that this notice is
 * included verbatim in any distributions. Additionally, the authors grant
 * permission to modify this software and its documentation for any purpose,
 * provided that such modifications are not distributed without the explicit
 * consent of the authors and that existing copyright notices are retained in
 * all copies. Some of the algorithms implemented by this software are
 * patented, observe all applicable patent law.
 *
 * IN NO EVENT SHALL THE AUTHORS OR DISTRIBUTORS BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF,
 * EVEN IF THE AUTHORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE AUTHORS AND DISTRIBUTORS SPECIFICALLY DISCLAIM ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.  THIS SOFTWARE IS
 * PROVIDED ON AN "AS IS" BASIS, AND THE AUTHORS AND DISTRIBUTORS HAVE NO
 * OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS.
 */