'use strict';

const required = ['CSC_LINK', 'CSC_KEY_PASSWORD'];
const missing = required.filter(name => !String(process.env[name] || '').trim());

if (missing.length) {
  console.error(`Signature Windows impossible : variable(s) manquante(s) ${missing.join(', ')}.`);
  console.error('Fournissez un certificat Authenticode PKCS#12 via CSC_LINK et son mot de passe via CSC_KEY_PASSWORD.');
  process.exit(1);
}

console.log('Matériel de signature détecté. Le build exigera une signature Authenticode valide.');
